package com.bor96dev.glue.presentation

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

    GlueScreen(
        title = state.title,
        totalDurationMs = state.totalDurationMs,
        currentTimeProvider = { currentTime },
        videoMoments = state.videoMoments,
        audioTracks = state.audioTracks,
        player = viewModel.player,
        isMerging = state.isMerging,
        isExporting = state.isExporting,
        exportSuccess = state.exportSuccess,
        error = state.error,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}