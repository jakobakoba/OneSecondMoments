package com.bor96dev.edit.presentation.event


sealed interface EditEvent {
    data class OnSeekChanged(val positionMs: Long) : EditEvent
    data class LocationPermissionResult(val granted: Boolean) : EditEvent
    object SaveClicked : EditEvent
    object OnBackClicked : EditEvent
}