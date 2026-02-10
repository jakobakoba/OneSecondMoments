package com.bor96dev.edit.presentation.event

import android.net.Uri


sealed interface EditEvent {
    data class OnVideoLoaded(val uri: Uri, val duration: Long): EditEvent
    data class OnSeekChanged(val positionMs: Long) : EditEvent
    object TogglePlayPause: EditEvent
    object SaveClicked: EditEvent
    object OnBackClicked: EditEvent
}