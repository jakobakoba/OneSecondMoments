package com.bor96dev.record.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bor96dev.record.presentation.event.RecordEvent
import com.bor96dev.ui.R

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

@Composable
fun RecordScreen(
    onVideoRecorded: (Uri, Long) -> Unit,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onEvent(RecordEvent.PermissionResult(granted = allGranted))
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onVideoRecorded(it, System.currentTimeMillis())
        }
    }

    LaunchedEffect(Unit) {
        val needsPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsPermissions.isNotEmpty()) {
            permissionLauncher.launch(needsPermissions.toTypedArray())
        } else {
            viewModel.onEvent(RecordEvent.PermissionResult(granted = true))
        }
    }

    LaunchedEffect(state.hasPermissions) {
        if (state.hasPermissions) {
            viewModel.bindCamera(lifecycleOwner)
        }
    }

    DisposableEffect(lifecycleOwner, state.hasPermissions) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (state.hasPermissions) {
                        viewModel.bindCamera(lifecycleOwner)
                    }
                }

                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onStop()
        }
    }

    LaunchedEffect(state.lastRecordedUri, state.recordedDate) {
        val uri = state.lastRecordedUri
        val date = state.recordedDate
        if (uri != null && date != null) {
            onVideoRecorded(uri, date)
            viewModel.onEvent(RecordEvent.OnNavigationDone)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                val isLandscape = (orientation in 60..120) || (orientation in 240..300)
                viewModel.onEvent(RecordEvent.OrientationChanged(isLandscape, rotation))
            }
        }
        listener.enable()
        onDispose {
            listener.disable()
        }
    }

    Box(modifier = Modifier.fillMaxSize())
    {
        if (state.hasPermissions) {
            val preview = state.videoPreview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        preview?.setSurfaceProvider(surfaceProvider)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!state.isLandscape) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rotate),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.record_screen_rotate_to_landscape),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            if (state.isLandscape) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            alpha =
                                if (state.isProcessing || (state.isRecording && !state.canStop)) 0.5f else 1f
                        }
                        .background(
                            color = if (state.isRecording) Color.Red else Color.White,
                            shape = CircleShape
                        )
                        .clickable(
                            enabled = !state.isProcessing && state.lastRecordedUri == null && (!state.isRecording || state.canStop)
                        ) { viewModel.onEvent(RecordEvent.ToggleRecording) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (state.isRecording) 32.dp else 60.dp)
                            .background(
                                color = if (state.isRecording) Color.Black else Color.Red,
                                shape = if (state.isRecording) RoundedCornerShape(8.dp) else CircleShape
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        galleryLauncher.launch("video/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.gallery),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
