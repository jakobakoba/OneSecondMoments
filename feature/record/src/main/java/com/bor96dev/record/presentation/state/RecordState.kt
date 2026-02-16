package com.bor96dev.record.presentation.state

import android.net.Uri
import androidx.camera.core.Preview

data class RecordState (
    val isLandscape: Boolean = false,
    val hasPermissions: Boolean = false,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val lastRecordedUri: Uri? = null,
    val recordedDate: Long? = null,
    val error: String? = null,
    val videoPreview: Preview? = null,
    val canStop: Boolean = false
)