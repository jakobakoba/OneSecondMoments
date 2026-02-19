package com.bor96dev.glue.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun GlueScreenRoute(
    monthQuery: String?,
    year: Int?,
    onBack: () -> Unit,
    viewModel: GlueViewModel = hiltViewModel<GlueViewModel, GlueViewModel.Factory> {factory ->
        factory.create(monthQuery, year)
    }
) {
    GlueScreen(
        state =state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )

}