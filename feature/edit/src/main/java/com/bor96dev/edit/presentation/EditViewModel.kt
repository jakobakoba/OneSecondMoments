package com.bor96dev.edit.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bor96dev.edit.presentation.event.EditEvent
import com.bor96dev.edit.presentation.state.EditState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    playerBuilder: ExoPlayer.Builder
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditState())
    val uiState = _uiState.asStateFlow()

    val player: Player = playerBuilder.build()

    fun onEvent(event: EditEvent){

    }


}


