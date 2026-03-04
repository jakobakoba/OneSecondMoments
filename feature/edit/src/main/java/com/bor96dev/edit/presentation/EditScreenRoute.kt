package com.bor96dev.edit.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    viewModel: EditViewModel = hiltViewModel<EditViewModel, EditViewModel.Factory>(
        key = navId.toString()
    ) { factory ->
        factory.create(videoUri = videoUri, date = date)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        viewModel.onEvent(EditEvent.LocationPermissionResult(granted = granted))
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onStart()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onStart()
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            viewModel.onStop()
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        val needsPermissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsPermissions.isNotEmpty()) {
            permissionLauncher.launch(needsPermissions.toTypedArray())
        } else {
            viewModel.onEvent(EditEvent.LocationPermissionResult(granted = true))
        }
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            onBack()
        }
    }

    EditScreen(
        state = state,
        player = player,
        onEvent = { event ->
            if (event is EditEvent.OnBackClicked) {
                onBack()
            } else {
                viewModel.onEvent(event)
            }
        }
    )
}
