package com.bor96dev.glue.presentation

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
@Composable
fun GlueScreenRoute(
    monthQuery: String?,
    year: Int?,
    onBack: () -> Unit,
    viewModel: GlueViewModel = hiltViewModel<GlueViewModel, GlueViewModel.Factory> { factory ->
        factory.create(monthQuery, year)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTimeMs.collectAsStateWithLifecycle()

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
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    GlueScreen(
        title = state.title,
        totalDurationMs = state.totalDurationMs,
        currentTimeProvider = { currentTime },
        videoMoments = state.videoMoments,
        audioTracks = state.audioTracks,
        player = player,
        isPlaying = state.isPlaying,
        isMerging = state.isMerging,
        mergeProgress = state.mergeProgress,
        isExporting = state.isExporting,
        exportSuccess = state.exportSuccess,
        error = state.error,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}