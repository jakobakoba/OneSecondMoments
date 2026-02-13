package com.bor96dev.record.presentation

import android.Manifest
import android.R
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bor96dev.record.presentation.event.RecordEvent

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

@Composable
fun RecordScreen(
    onVideoRecorded: (Uri) -> Unit,
    viewModel: RecordViewModel = viewModel()
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

    LaunchedEffect(state.hasPermissions){
        if(state.hasPermissions){
            viewModel.bindCamera(lifecycleOwner)
        }
    }

    LaunchedEffect(state.lastRecordedUri){
        state.lastRecordedUri?.let {uri ->
            onVideoRecorded(uri)
            viewModel.onEvent(RecordEvent.OnNavigationDone)
        }
    }

    Box(modifier = Modifier.fillMaxSize())
    {
        if (state.hasPermissions) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    state.videoPreview?.setSurfaceProvider(previewView.surfaceProvider)
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "February 5, 2026",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "San Francisco, CA",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, CircleShape)
                    .clickable { viewModel.onEvent(RecordEvent.ToggleRecording) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (state.isRecording) 40.dp else 60.dp)
                        .background(
                            Color.Red,
                            if (state.isRecording) RoundedCornerShape(8.dp) else CircleShape
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu_gallery),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}