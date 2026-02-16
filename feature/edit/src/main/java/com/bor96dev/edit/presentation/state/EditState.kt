package com.bor96dev.edit.presentation.state

import android.net.Uri

data class EditState(
    val videoUri: Uri? = null,
    val isPlaying: Boolean = false,
    val videoDurationMs: Long = 0,
    val selectedStartMs: Long = 0,
    val dateText: String = "",
    val locationText: String? = null,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val error: String? = null
)