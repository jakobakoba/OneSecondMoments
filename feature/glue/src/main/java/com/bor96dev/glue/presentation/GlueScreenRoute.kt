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
    GlueScreen(
        state =state,
        player = viewModel.player,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )

}