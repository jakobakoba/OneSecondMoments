package com.bor96dev.glue.presentation

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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
                    syncMusicPlayback(forceSeek = true)
                } else {
                    stopProgressTracking()
                    musicPlayer.pause()
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
                syncMusicPlayback(forceSeek = true)
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _currentTimeMs.value = getCurrentTimelinePosition()
                delay(33)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }


    @OptIn(UnstableApi::class)
    private fun getCurrentTimelinePosition(): Long {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return player.currentPosition

        var position = 0L
        val window = Timeline.Window()
        for (i in 0 until player.currentMediaItemIndex) {
            timeline.getWindow(i, window)
            if (window.durationUs != androidx.media3.common.C.TIME_UNSET) {
                position += window.durationMs
            } else {
                position += 1000L
            }
        }
        return position + player.currentPosition
    }

    private fun syncMusicPlayback(forceSeek: Boolean = false) {
        val currentPos = getCurrentTimelinePosition()
        val track = _uiState.value.audioTracks.firstOrNull() ?: return

        val musicPos = currentPos - track.startInTimelineMs
        val trackDuration = track.endInTimelineMs - track.startInTimelineMs

        if (musicPos in 0 until trackDuration) {
            if (musicPlayer.currentMediaItem?.localConfiguration?.uri != track.uri) {
                musicPlayer.setMediaItem(MediaItem.fromUri(track.uri))
                musicPlayer.prepare()
            }

            val targetMusicPos = track.trimStartMs + musicPos
            val drift = abs(musicPlayer.currentPosition - targetMusicPos)

            if (forceSeek || drift > 200) {
                musicPlayer.seekTo(targetMusicPos)
            }
            if (player.isPlaying && !musicPlayer.isPlaying) {
                musicPlayer.play()
            }
        } else {
            musicPlayer.pause()
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
                val track = AudioTrack(
                    uri = event.uri,
                    name = event.name,
                    startInTimelineMs = 0,
                    endInTimelineMs = _uiState.value.totalDurationMs,
                    volume = 1.0f
                )

                _uiState.update { it.copy(audioTracks = listOf(track)) }

                musicPlayer.setMediaItem(MediaItem.fromUri(track.uri))
                musicPlayer.prepare()
                musicPlayer.volume = _uiState.value.musicVolume
                syncMusicPlayback(forceSeek = true)
            }

            is GlueEvent.OnSeekChanged -> {
                val itemIndex = (event.positionMs / 1000).toInt()
                    .coerceIn(0, _uiState.value.videoMoments.size - 1)
                val positionInItem = event.positionMs % 1000
                player.seekTo(itemIndex, positionInItem)
                _currentTimeMs.value = event.positionMs
                syncMusicPlayback(forceSeek = true)
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