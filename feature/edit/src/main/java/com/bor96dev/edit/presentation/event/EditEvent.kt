package com.bor96dev.edit.presentation.event

import android.net.Uri


sealed interface EditEvent {
    data class OnSeekChanged(val positionMs: Long) : EditEvent
    data class LocationPermissionResult(val granted: Boolean) : EditEvent
    object TogglePlay: EditEvent
    object SaveClicked: EditEvent
    object OnBackClicked: EditEvent
}