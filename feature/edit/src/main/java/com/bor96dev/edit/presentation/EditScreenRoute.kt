package com.bor96dev.edit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bor96dev.edit.presentation.event.EditEvent


@Composable
fun EditScreenRoute(
    videoUri: String,
    date: Long,
    navId: Long,
    onBack: () -> Unit,
    viewModel: EditViewModel = hiltViewModel<EditViewModel, EditViewModel.Factory> (
        key = navId.toString()
    ) { factory ->
        factory.create(videoUri = videoUri, date = date)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onStart()
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.saveCompleted){
        if (state.saveCompleted){
            onBack()
        }
    }

    EditScreen(
        state = state,
        player = player,
        onEvent = { event ->
            if (event is EditEvent.OnBackClicked){
                onBack()
            } else {
                viewModel.onEvent(event)
            }
        }
    )
}