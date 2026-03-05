package com.bor96dev.glue.presentation

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.bor96dev.glue.domain.GlueRepository
import com.bor96dev.glue.presentation.event.GlueEvent
import com.bor96dev.glue.presentation.state.AudioTrack
import com.bor96dev.glue.presentation.state.GlueState
import com.bor96dev.glue.presentation.timeline.clampedEndForGap
import com.bor96dev.glue.presentation.timeline.findFirstGap
import com.bor96dev.glue.presentation.timeline.hasSpaceForNewAudio
import com.bor96dev.glue.presentation.timeline.toTrackRanges
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Provider

@UnstableApi
@HiltViewModel(assistedFactory = GlueViewModel.Factory::class)
class GlueViewModel @OptIn(UnstableApi::class)
@AssistedInject constructor(
    @Assisted private val monthQuery: String?,
    @Assisted private val year: Int?,
    private val repository: GlueRepository,
    @ApplicationContext private val context: Context,
    private val playerBuilderProvider: Provider<ExoPlayer.Builder>
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(monthQuery: String?, year: Int?): GlueViewModel
    }

    private val _uiState = MutableStateFlow(GlueState())
    val uiState = _uiState.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    private var internalPlayer: ExoPlayer? = null
    private val _playerFlow = MutableStateFlow<ExoPlayer?>(null)
    val playerFlow = _playerFlow.asStateFlow()

    private val musicPlayers = mutableMapOf<String, ExoPlayer>()

    private var progressJob: Job? = null
    private var premergeProgressJob: Job? = null
    private var exportProgressJob: Job? = null
    private var premergeTransformer: Transformer? = null
    private var exportTransformer: Transformer? = null

    private var premergedVideoPath: String? = null

    private var savedPositionMs: Long = 0L

    init {
        loadMoments()
    }

    fun onStart() {
        if (internalPlayer == null) {
            initializePlayers()
        }
    }

    fun onStop() {
        releasePlayers()
    }

    private fun initializePlayers() {
        val newPlayer = playerBuilderProvider.get().build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }

        internalPlayer = newPlayer
        setupPlayerListeners(newPlayer)

        premergedVideoPath?.let { path ->
            newPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            newPlayer.prepare()
            newPlayer.seekTo(savedPositionMs)
        }

        val audioTracks = _uiState.value.audioTracks
        audioTracks.forEach { track ->
            val musicPlayer = playerBuilderProvider.get().build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
            musicPlayer.setMediaItem(MediaItem.fromUri(track.uri))
            musicPlayer.prepare()
            musicPlayers[track.id] = musicPlayer
        }

        _playerFlow.value = newPlayer
    }

    private fun releasePlayers() {
        internalPlayer?.let {
            savedPositionMs = it.currentPosition
            it.stop()
            it.release()
        }
        internalPlayer = null
        _playerFlow.value = null

        musicPlayers.values.forEach {
            it.stop()
            it.release()
        }
        musicPlayers.clear()

        stopProgressTracking()
    }

    private fun loadMoments() {
        viewModelScope.launch {
            val flow = when {
                monthQuery != null -> repository.getMomentsByMonth(monthQuery)
                year != null -> repository.getMomentsByYear(year)
                else -> return@launch
            }

            flow.collect { moments ->
                _uiState.update {
                    it.copy(
                        videoMoments = moments,
                        totalDurationMs = moments.size * 1000L,
                        title = monthQuery ?: year?.toString() ?: ""
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        hasSpaceForNewAudio = hasSpaceForNewAudio(
                            state.audioTracks.toTrackRanges(),
                            state.totalDurationMs
                        )
                    )
                }
                startPremerge(moments.map { it.videoUri })
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun startPremerge(videoUris: List<String>) {
        if (videoUris.isEmpty()) return

        val title = monthQuery ?: year?.toString() ?: "output"
        val cacheFile = File(context.cacheDir, "premerge_$title.mp4")

        if (cacheFile.exists()) cacheFile.delete()

        _uiState.update { it.copy(isMerging = true, mergeProgress = 0f) }

        val editedItems = videoUris.map { uri ->
            EditedMediaItem.Builder(MediaItem.fromUri(uri))
                .setRemoveAudio(false)
                .build()
        }

        val sequence = EditedMediaItemSequence.Builder(
            setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO)
        )
            .addItems(*editedItems.toTypedArray())
            .build()

        val composition = Composition.Builder(listOf(sequence)).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    premergedVideoPath = cacheFile.absolutePath
                    _uiState.update { it.copy(isMerging = false) }

                    internalPlayer?.let { player ->
                        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(cacheFile)))
                        player.prepare()
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    _uiState.update {
                        it.copy(
                            isMerging = false,
                            error = "Preview preparation failed: ${exportException.message}"
                        )
                    }
                    internalPlayer?.let { player ->
                        val mediaItems =
                            _uiState.value.videoMoments.map { MediaItem.fromUri(it.videoUri) }
                        player.setMediaItems(mediaItems)
                        player.prepare()
                    }
                }
            })
            .build()
        premergeTransformer = transformer

        transformer.start(composition, cacheFile.absolutePath)

        premergeProgressJob?.cancel()
        premergeProgressJob = viewModelScope.launch(Dispatchers.Main) {
            val progressHolder = ProgressHolder()
            while (_uiState.value.isMerging) {
                val state = premergeTransformer?.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _uiState.update { it.copy(mergeProgress = progressHolder.progress / 100f) }
                }
                delay(200)
            }
        }
    }

    private fun setupPlayerListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                    if (!player.playWhenReady || player.playbackState == Player.STATE_ENDED) {
                        musicPlayers.values.forEach { it.pause() }
                    }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) return
                syncMusicPlayback(currentPos = player.currentPosition, forceSeek = true)
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val p = internalPlayer ?: break
                val currentPos = p.currentPosition
                _currentTimeMs.value = currentPos
                syncMusicPlayback(currentPos = currentPos, forceSeek = false)
                delay(33)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    private fun syncMusicPlayback(currentPos: Long, forceSeek: Boolean = false) {
        val mainPlayer = internalPlayer ?: return
        val state = _uiState.value
        val activeTrackIds = mutableSetOf<String>()

        for (track in state.audioTracks) {
            val musicPlayer = musicPlayers[track.id] ?: continue
            val isInRange = currentPos in track.startInTimelineMs..track.endInTimelineMs

            if (isInRange) {
                activeTrackIds.add(track.id)

                if (musicPlayer.currentMediaItem?.localConfiguration?.uri != track.uri) {
                    musicPlayer.setMediaItem(MediaItem.fromUri(track.uri))
                    musicPlayer.prepare()
                    continue
                }

                if (musicPlayer.playbackState == Player.STATE_BUFFERING) continue

                val targetMusicPos = track.trimStartMs + (currentPos - track.startInTimelineMs)

                if (forceSeek) {
                    musicPlayer.seekTo(targetMusicPos)
                    if (mainPlayer.isPlaying) {
                        musicPlayer.playWhenReady = true
                    }
                } else if (!musicPlayer.isPlaying && mainPlayer.isPlaying) {
                    musicPlayer.seekTo(targetMusicPos)
                    musicPlayer.playWhenReady = true
                }
            } else {
                if (musicPlayer.isPlaying) musicPlayer.pause()
            }
        }

        musicPlayers.forEach { (id, p) ->
            if (id !in activeTrackIds && p.isPlaying) p.pause()
        }
    }

    @OptIn(UnstableApi::class)
    private fun doExport() {
        val mergedPath = premergedVideoPath
        if (mergedPath == null) {
            _uiState.update { it.copy(error = "Video is not ready yet, please wait") }
            return
        }

        _uiState.update { it.copy(isExporting = true, exportProgress = 0f, error = null) }

        val state = _uiState.value
        val title = state.title
        val outputFile = File(context.cacheDir, "export_$title.mp4")
        if (outputFile.exists()) outputFile.delete()

        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(File(mergedPath))))
            .build()

        val sequences = mutableListOf<EditedMediaItemSequence>()

        val videoSequence = EditedMediaItemSequence.Builder(
            setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO)
        )
            .addItem(videoItem)
            .build()
        sequences.add(videoSequence)

        for (track in state.audioTracks) {
            val audioSequenceItems = mutableListOf<EditedMediaItem>()

            if (track.startInTimelineMs > 0) {
                val silentMixer = androidx.media3.common.audio.ChannelMixingAudioProcessor()
                silentMixer.putChannelMixingMatrix(
                    androidx.media3.common.audio.ChannelMixingMatrix(
                        1, 1, floatArrayOf(0f)
                    )
                )
                silentMixer.putChannelMixingMatrix(
                    androidx.media3.common.audio.ChannelMixingMatrix(
                        2, 2, floatArrayOf(0f, 0f, 0f, 0f)
                    )
                )
                val silentClip = EditedMediaItem.Builder(
                    MediaItem.Builder()
                        .setUri(track.uri)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(track.trimStartMs)
                                .setEndPositionMs(track.trimStartMs + track.startInTimelineMs)
                                .build()
                        )
                        .build()
                )
                    .setRemoveVideo(true)
                    .setEffects(
                        androidx.media3.transformer.Effects(listOf(silentMixer), emptyList())
                    )
                    .build()
                audioSequenceItems.add(silentClip)
            }

            val audioDuration = track.endInTimelineMs - track.startInTimelineMs
            val audioClip = EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(track.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(track.trimStartMs)
                            .setEndPositionMs(track.trimStartMs + audioDuration)
                            .build()
                    )
                    .build()
            )
                .setRemoveVideo(true)
                .build()
            audioSequenceItems.add(audioClip)

            val audioSequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
                .addItems(*audioSequenceItems.toTypedArray())
                .build()
            sequences.add(audioSequence)
        }

        val composition = Composition.Builder(sequences).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    viewModelScope.launch {
                        val savedUri = saveToGallery(outputFile, title)
                        outputFile.delete()
                        if (savedUri != null) {
                            Toast.makeText(context, "Saved to gallery", Toast.LENGTH_LONG).show()
                            launchVideoViewer(savedUri)
                            _uiState.update { it.copy(isExporting = false) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    error = "Failed to save to gallery"
                                )
                            }
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            error = "Export failed: ${exportException.message}"
                        )
                    }
                }
            })
            .build()
        exportTransformer = transformer

        transformer.start(composition, outputFile.absolutePath)

        exportProgressJob?.cancel()
        exportProgressJob = viewModelScope.launch(Dispatchers.Main) {
            val progressHolder = ProgressHolder()
            while (_uiState.value.isExporting) {
                val state = exportTransformer?.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _uiState.update { it.copy(exportProgress = progressHolder.progress / 100f) }
                }
                delay(200)
            }
        }
    }

    private suspend fun saveToGallery(file: File, title: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val moviesDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "OneSecondMoments"
                    )
                    if (!moviesDir.exists()) {
                        moviesDir.mkdirs()
                    }
                }

                val resolver = context.contentResolver
                val fileName = "OneSecondMoments_${title}_${System.currentTimeMillis()}.mp4"
                val nowMs = System.currentTimeMillis()
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/OneSecondMoments"
                    )
                    put(MediaStore.Video.Media.DATE_TAKEN, nowMs)
                    put(MediaStore.Video.Media.DATE_ADDED, nowMs / 1000)
                    put(MediaStore.Video.Media.DATE_MODIFIED, nowMs / 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext null

                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                uri
            } catch (_: Exception) {
                null
            }
        }

    fun onEvent(event: GlueEvent) {
        val p = internalPlayer ?: return

        when (event) {
            is GlueEvent.TogglePlay -> {
                if (p.isPlaying) {
                    p.pause()
                } else {
                    if (p.playbackState == Player.STATE_ENDED) {
                        p.seekTo(0)
                    }
                    p.play()
                }
            }

            is GlueEvent.OnAudioAdded -> {
                val currentTracks = _uiState.value.audioTracks
                if (currentTracks.size >= 5) return

                viewModelScope.launch {
                    val duration = withContext(Dispatchers.IO) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, event.uri)
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLong() ?: 0L
                        } catch (_: Exception) {
                            0L
                        } finally {
                            retriever.release()
                        }
                    }

                    val state = _uiState.value
                    val totalDur = state.totalDurationMs
                    val clampedDuration = minOf(duration, totalDur)
                    val trackRanges = state.audioTracks.toTrackRanges()

                    val gapStart = findFirstGap(trackRanges, clampedDuration, totalDur)
                    val desiredEnd = gapStart + clampedDuration
                    val actualEnd =
                        clampedEndForGap(gapStart, trackRanges, desiredEnd, totalDur)

                    val newTrack = AudioTrack(
                        uri = event.uri,
                        name = event.name,
                        fileDurationMs = duration,
                        startInTimelineMs = gapStart,
                        endInTimelineMs = actualEnd
                    )

                    val newMusicPlayer = playerBuilderProvider.get().build().apply {
                        repeatMode = Player.REPEAT_MODE_OFF
                    }
                    musicPlayers[newTrack.id] = newMusicPlayer

                    _uiState.update {
                        val updatedTracks = it.audioTracks + newTrack
                        it.copy(
                            audioTracks = updatedTracks,
                            hasSpaceForNewAudio = hasSpaceForNewAudio(
                                updatedTracks.toTrackRanges(),
                                it.totalDurationMs
                            )
                        )
                    }
                }
            }

            is GlueEvent.OnAudioRemoved -> {
                musicPlayers[event.trackId]?.release()
                musicPlayers.remove(event.trackId)
                _uiState.update {
                    val updatedTracks = it.audioTracks.filter { t -> t.id != event.trackId }
                    it.copy(
                        audioTracks = updatedTracks,
                        hasSpaceForNewAudio = hasSpaceForNewAudio(
                            updatedTracks.toTrackRanges(),
                            it.totalDurationMs
                        )
                    )
                }
            }

            is GlueEvent.OnSeekChanged -> {
                p.seekTo(event.positionMs)
                _currentTimeMs.value = event.positionMs
                syncMusicPlayback(currentPos = event.positionMs, forceSeek = true)
            }

            is GlueEvent.OnAudioUpdate -> {
                _uiState.update { state ->
                    val otherTracks = state.audioTracks.filter { it.id != event.trackId }
                    val wouldOverlap = otherTracks.any { other ->
                        event.startMs < other.endInTimelineMs && event.endMs > other.startInTimelineMs
                    }
                    if (wouldOverlap) {
                        state
                    } else {
                        val updatedTracks = state.audioTracks.map { track ->
                            if (track.id == event.trackId) {
                                track.copy(
                                    startInTimelineMs = event.startMs,
                                    endInTimelineMs = event.endMs,
                                    trimStartMs = event.trimStartMs
                                )
                            } else track
                        }
                        state.copy(
                            audioTracks = updatedTracks,
                            hasSpaceForNewAudio = hasSpaceForNewAudio(
                                updatedTracks.toTrackRanges(),
                                state.totalDurationMs
                            )
                        )
                    }
                }
                syncMusicPlayback(p.currentPosition, forceSeek = true)
            }

            is GlueEvent.OnExportClicked -> {
                stopPlayback()
                doExport()
            }
        }
    }

    private fun stopPlayback() {
        internalPlayer?.let {
            if (it.isPlaying) it.pause()
        }
        musicPlayers.values.forEach {
            if (it.isPlaying) it.pause()
        }
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun launchVideoViewer(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }


    @OptIn(UnstableApi::class)
    override fun onCleared() {
        releasePlayers()
        premergeTransformer?.cancel()
        exportTransformer?.cancel()
        premergedVideoPath?.let { File(it).delete() }
        super.onCleared()
    }
}