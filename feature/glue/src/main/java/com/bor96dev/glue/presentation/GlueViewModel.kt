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
import javax.inject.Provider
import kotlin.math.abs

@HiltViewModel(assistedFactory = GlueViewModel.Factory::class)
class GlueViewModel @UnstableApi
@AssistedInject constructor(
    @Assisted private val monthQuery: String?,
    @Assisted private val year: Int?,
    private val repository: GlueRepository,
    @ApplicationContext private val context: Context,
    playerBuilderProvider: Provider<ExoPlayer.Builder>
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlueState())
    val uiState = _uiState.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    val player = playerBuilderProvider.get().build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
    }

    private val musicPlayer = playerBuilderProvider.get().build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
    }

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
                        musicPlayer.pause()
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

    private fun syncMusicPlayback(currentPos: Long, forceSeek: Boolean = false) {
        val track = _uiState.value.audioTracks.firstOrNull()

        if (track == null) {
            if (musicPlayer.isPlaying) musicPlayer.pause()
            return
        }

        val isInRange = currentPos in track.startInTimelineMs..track.endInTimelineMs

        if (isInRange) {
            if (musicPlayer.currentMediaItem?.localConfiguration?.uri != track.uri) {
                musicPlayer.setMediaItem(MediaItem.fromUri(track.uri))
                musicPlayer.prepare()
                return
            }

            if (musicPlayer.playbackState == Player.STATE_BUFFERING) {
                return
            }

            val targetMusicPos = track.trimStartMs + (currentPos - track.startInTimelineMs)
            val drift = abs(musicPlayer.currentPosition - targetMusicPos)

            if (forceSeek || drift > 200) {
                musicPlayer.seekTo(targetMusicPos)
            }
            if (player.isPlaying && !musicPlayer.isPlaying) {
                musicPlayer.play()
            }

            musicPlayer.volume = _uiState.value.musicVolume
        } else {
            if (musicPlayer.isPlaying) {
                musicPlayer.pause()
            }
        }
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
                musicPlayer.volume = event.volume
            }

            is GlueEvent.OnVideoVolumeChanged -> {
                _uiState.update { it.copy(videoVolume = event.volume) }
                player.volume = event.volume
            }

            is GlueEvent.OnAudioAdded -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, event.uri)
                        val duration =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLong() ?: 0L

                        val newTrack = AudioTrack(
                            uri = event.uri,
                            name = event.name,
                            fileDurationMs = duration,
                            startInTimelineMs = 0,
                            endInTimelineMs = minOf(duration, _uiState.value.totalDurationMs)
                        )
                        _uiState.update { it.copy(audioTracks = it.audioTracks + newTrack) }
                    } catch (_: Exception) {

                    } finally {
                        retriever.release()
                    }
                }
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
                    state.copy(
                        audioTracks = state.audioTracks.map { track ->
                            if (track.id == event.trackId) {
                                track.copy(
                                    startInTimelineMs = event.startMs,
                                    endInTimelineMs = event.endMs,
                                    trimStartMs = event.trimStartMs
                                )
                            } else {
                                track
                            }
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
        musicPlayer.release()
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(monthQuery: String?, year: Int?): GlueViewModel
    }
}