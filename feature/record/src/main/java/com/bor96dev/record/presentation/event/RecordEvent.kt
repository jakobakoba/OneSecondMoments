package com.bor96dev.record.presentation.event

sealed interface RecordEvent {
    data class PermissionResult(val granted: Boolean): RecordEvent
    object ToggleRecording: RecordEvent
    object OnNavigationDone: RecordEvent
}