package com.bor96dev.glue.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bor96dev.glue.domain.GlueRepository
import com.bor96dev.glue.presentation.event.GlueEvent
import com.bor96dev.glue.presentation.state.GlueState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = GlueViewModel.Factory::class)
class GlueViewModel @UnstableApi
@AssistedInject constructor(
    @Assisted private val monthQuery: String?,
    @Assisted private val year: Int?,
    private val repository: GlueRepository,
    playerBuilder: ExoPlayer.Builder
) : ViewModel() {
    private val _uiState = MutableStateFlow(GlueState())
    val uiState = _uiState.asStateFlow()

    val player = playerBuilder.build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
    }

    init {
        loadMoments()
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

    fun onEvent(event: GlueEvent) {
        when (event) {
            is GlueEvent.TogglePlay -> {
                if (player.isPlaying) player.pause() else player.play()
                _uiState.update { it.copy(isPlaying = player.isPlaying) }
            }

            else -> {}
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(monthQuery: String?, year: Int?): GlueViewModel
    }
}