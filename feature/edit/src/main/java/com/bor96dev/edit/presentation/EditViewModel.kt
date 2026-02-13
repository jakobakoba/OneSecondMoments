package com.bor96dev.edit.presentation

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bor96dev.edit.presentation.event.EditEvent
import com.bor96dev.edit.presentation.state.EditState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel(assistedFactory = EditViewModel.Factory::class)
class EditViewModel @AssistedInject constructor(
    @Assisted private val videoUri: String,
    playerBuilder: ExoPlayer.Builder
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(videoUri: String): EditViewModel
    }

    private val _uiState = MutableStateFlow(EditState())
    val uiState = _uiState.asStateFlow()

    val player: Player = playerBuilder.build()

    init {

        Log.d("GTA5", "loading video: $videoUri")

        player.repeatMode = Player.REPEAT_MODE_OFF
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        })

        val uri = videoUri.toUri()
        _uiState.update { it.copy(videoUri = uri) }
        setupInitialPlayer(uri)
    }

    private fun setupInitialPlayer(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update { it.copy(videoDurationMs = player.duration) }
                    updatePlayerClipping(uri, 0L)
                    player.removeListener(this)
                }
            }
        })
    }

    private fun updatePlayerClipping(uri: Uri, startMs: Long) {
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(startMs + 1000)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(clipping)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun onEvent(event: EditEvent) {
        when (event) {
            is EditEvent.OnSeekChanged -> {
                _uiState.update { it.copy(selectedStartMs = event.positionMs) }
                _uiState.value.videoUri?.let { uri ->
                    updatePlayerClipping(uri, event.positionMs)
                }
            }

            is EditEvent.TogglePlay -> {
                player.seekTo(0)
                player.play()
            }

            else -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}


