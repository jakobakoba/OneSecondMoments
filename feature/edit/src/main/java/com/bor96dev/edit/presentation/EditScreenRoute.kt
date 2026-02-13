package com.bor96dev.edit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bor96dev.edit.presentation.event.EditEvent


@Composable
fun EditScreenRoute(
    videoUri: String,
    onBack: () -> Unit,
    viewModel: EditViewModel = hiltViewModel<EditViewModel, EditViewModel.Factory> {factory ->
        factory.create(videoUri = videoUri)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EditScreen(
        state = state,
        player = viewModel.player,
        onEvent = { event ->
            if (event is EditEvent.OnBackClicked){
                onBack()
            } else {
                viewModel.onEvent(event)
            }
        }
    )
}