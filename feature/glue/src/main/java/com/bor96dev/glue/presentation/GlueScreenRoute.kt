package com.bor96dev.glue.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GlueScreenRoute(
    monthQuery: String?,
    year: Int?,
    onBack: () -> Unit,
    viewModel: GlueViewModel = hiltViewModel<GlueViewModel, GlueViewModel.Factory> {factory ->
        factory.create(monthQuery, year)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTimeMs.collectAsStateWithLifecycle()

    GlueScreen(
        title = state.title,
        videoVolume = state.videoVolume,
        musicVolume = state.musicVolume,
        totalDurationMs = state.totalDurationMs,
        currentTimeProvider = {currentTime},
        videoMoments = state.videoMoments,
        audioTracks = state.audioTracks,
        player = viewModel.player,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )

}