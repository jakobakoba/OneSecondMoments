package com.bor96dev.glue.presentation

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Provider

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
                    syncMusicPlayback()
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
                syncMusicPlayback()
            }
        })
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(currentTimeMs = player.currentPosition) }
                delay(33)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    private fun syncMusicPlayback() {
        val currentPos = player.currentPosition
        val track = _uiState.value.audioTracks.firstOrNull() ?: return

        val musicPos = currentPos - track.startInTimelineMs
        if (musicPos in 0 until (track.endInTimelineMs - track.startInTimelineMs)) {
            if (musicPlayer.currentMediaItem?.localConfiguration?.uri != track.uri) {
                musicPlayer.setMediaItem(MediaItem.fromUri(track.uri))
                musicPlayer.prepare()
            }
            musicPlayer.seekTo(track.trimStartMs + musicPos)
            if (player.isPlaying) musicPlayer.play()
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
            }

            is GlueEvent.OnSeekChanged -> {
                player.seekTo(event.positionMs)
                _uiState.update { it.copy(currentTimeMs = event.positionMs) }
                syncMusicPlayback()
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