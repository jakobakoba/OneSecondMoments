package com.bor96dev.glue.presentation

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bor96dev.glue.domain.GlueRepository
import com.bor96dev.glue.presentation.event.GlueEvent
import com.bor96dev.glue.presentation.state.AudioTrack
import com.bor96dev.glue.presentation.state.GlueState
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
import javax.inject.Provider
import kotlin.math.abs

@HiltViewModel(assistedFactory = GlueViewModel.Factory::class)
class GlueViewModel @UnstableApi
@AssistedInject constructor(
    @Assisted private val monthQuery: String?,
    @Assisted private val year: Int?,
    private val repository: GlueRepository,
    @ApplicationContext private val context: Context,
    private val playerBuilderProvider: Provider<ExoPlayer.Builder>
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlueState())
    val uiState = _uiState.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    val player = playerBuilderProvider.get().build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
    }

    private val musicPlayers = mutableMapOf<String, ExoPlayer>()

    private var progressJob: Job? = null

    init {
        loadMoments()
        setupPlayerListeners()
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

                val mediaItems = moments.map { MediaItem.fromUri(it.videoUri) }
                player.setMediaItems(mediaItems)
                player.prepare()
            }
        }
    }

    private fun setupPlayerListeners() {
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
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    return
                }
                syncMusicPlayback(currentPos = getCurrentTimelinePosition(), forceSeek = true)
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val currentPos = getCurrentTimelinePosition()
                _currentTimeMs.value = currentPos
                syncMusicPlayback(currentPos = currentPos, forceSeek = false)
                delay(33)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    private fun getCurrentTimelinePosition(): Long {
        return (player.currentMediaItemIndex * 1000L) + player.currentPosition
    }

    private fun createMusicPlayer(): ExoPlayer {
        return playerBuilderProvider.get().build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    private fun syncMusicPlayback(currentPos: Long, forceSeek: Boolean = false) {
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

                if (musicPlayer.playbackState == Player.STATE_BUFFERING) {
                    continue
                }

                val targetMusicPos = track.trimStartMs + (currentPos - track.startInTimelineMs)
                val drift = abs(musicPlayer.currentPosition - targetMusicPos)

                if (forceSeek || drift > 200) {
                    musicPlayer.seekTo(targetMusicPos)
                }
                if (player.isPlaying && !musicPlayer.isPlaying) {
                    musicPlayer.play()
                }

                musicPlayer.volume = state.musicVolume
            } else {
                if (musicPlayer.isPlaying) {
                    musicPlayer.pause()
                }
            }
        }

        musicPlayers.forEach { (id, p) ->
            if (id !in activeTrackIds && p.isPlaying) p.pause()
        }
    }

    private fun findFirstGap(
        existingTracks: List<AudioTrack>,
        neededDurationMs: Long,
        totalDurationMs: Long
    ): Long {
        val sorted = existingTracks.sortedBy { it.startInTimelineMs }

        if (sorted.isEmpty()) return 0L
        if (sorted.first().startInTimelineMs >= neededDurationMs) return 0L

        for (i in 0 until sorted.size - 1) {
            val gapStart = sorted[i].endInTimelineMs
            val gapEnd = sorted[i + 1].startInTimelineMs
            if (gapEnd - gapStart >= neededDurationMs) return gapStart
        }

        val lastEnd = sorted.last().endInTimelineMs
        if (totalDurationMs - lastEnd >= neededDurationMs) return lastEnd

        return findLargestGapStart(sorted, totalDurationMs)
    }

    private fun findLargestGapStart(sorted: List<AudioTrack>, totalDurationMs: Long): Long {
        var bestStart = 0L
        var bestSize = sorted.first().startInTimelineMs

        for (i in 0 until sorted.size - 1) {
            val gapStart = sorted[i].endInTimelineMs
            val gapSize = sorted[i + 1].startInTimelineMs - gapStart
            if (gapSize > bestSize) {
                bestSize = gapSize
                bestStart = gapStart
            }
        }

        val afterLastSize = totalDurationMs - sorted.last().endInTimelineMs
        if (afterLastSize > bestSize) {
            bestStart = sorted.last().endInTimelineMs
        }

        return bestStart
    }

    private fun clampedEndForGap(
        gapStart: Long,
        existingTracks: List<AudioTrack>,
        desiredEndMs: Long,
        totalDurationMs: Long
    ): Long {
        val sorted = existingTracks.sortedBy { it.startInTimelineMs }
        val nextTrackStart = sorted.filter { it.startInTimelineMs > gapStart }
            .minOfOrNull { it.startInTimelineMs } ?: totalDurationMs
        return minOf(desiredEndMs, nextTrackStart)
    }

    fun onEvent(event: GlueEvent) {
        when (event) {
            is GlueEvent.TogglePlay -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0)
                    }
                    player.play()
                }
            }

            is GlueEvent.OnMusicVolumeChanged -> {
                _uiState.update { it.copy(musicVolume = event.volume) }
                musicPlayers.values.forEach { it.volume = event.volume }
            }

            is GlueEvent.OnVideoVolumeChanged -> {
                _uiState.update { it.copy(videoVolume = event.volume) }
                player.volume = event.volume
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

                    val gapStart = findFirstGap(state.audioTracks, clampedDuration, totalDur)
                    val desiredEnd = gapStart + clampedDuration
                    val actualEnd = clampedEndForGap(gapStart, state.audioTracks, desiredEnd, totalDur)

                    val newTrack = AudioTrack(
                        uri = event.uri,
                        name = event.name,
                        fileDurationMs = duration,
                        startInTimelineMs = gapStart,
                        endInTimelineMs = actualEnd
                    )

                    val newPlayer = createMusicPlayer()
                    musicPlayers[newTrack.id] = newPlayer

                    _uiState.update { it.copy(audioTracks = it.audioTracks + newTrack) }
                }
            }

            is GlueEvent.OnAudioRemoved -> {
                musicPlayers[event.trackId]?.release()
                musicPlayers.remove(event.trackId)
                _uiState.update { it.copy(audioTracks = it.audioTracks.filter { t -> t.id != event.trackId }) }
            }

            is GlueEvent.OnSeekChanged -> {
                val itemIndex = (event.positionMs / 1000).toInt()
                    .coerceIn(0, _uiState.value.videoMoments.size - 1)
                val positionInItem = event.positionMs % 1000
                player.seekTo(itemIndex, positionInItem)
                _currentTimeMs.value = event.positionMs
                syncMusicPlayback(currentPos = event.positionMs, forceSeek = true)
            }

            is GlueEvent.OnAudioUpdate -> {
                _uiState.update { state ->
                    val otherTracks = state.audioTracks.filter { it.id != event.trackId }
                    val wouldOverlap = otherTracks.any { other ->
                        event.startMs < other.endInTimelineMs && event.endMs > other.startInTimelineMs
                    }
                    if (wouldOverlap) state
                    else state.copy(
                        audioTracks = state.audioTracks.map { track ->
                            if (track.id == event.trackId) {
                                track.copy(
                                    startInTimelineMs = event.startMs,
                                    endInTimelineMs = event.endMs,
                                    trimStartMs = event.trimStartMs
                                )
                            } else track
                        }
                    )
                }
                syncMusicPlayback(getCurrentTimelinePosition(), forceSeek = true)
            }

            else -> {}
        }
    }

    override fun onCleared() {
        stopProgressTracking()
        player.release()
        musicPlayers.values.forEach { it.release() }
        musicPlayers.clear()
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(monthQuery: String?, year: Int?): GlueViewModel
    }
}