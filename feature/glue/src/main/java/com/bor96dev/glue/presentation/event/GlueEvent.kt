package com.bor96dev.glue.presentation.event

import android.net.Uri

sealed interface GlueEvent {
    data class OnAudioAdded(val uri: Uri, val name: String): GlueEvent
    data class OnAudioRemoved(val trackId: String) : GlueEvent
    data class OnAudioPositionChanged(val trackId: String) : GlueEvent
    data class OnAudioTrimChanged(val trackId: String): GlueEvent
    data class OnVideoVolumeChanged(val volume: Float): GlueEvent
    data class OnMusicVolumeChanged(val volume: Float): GlueEvent
    object TogglePlay: GlueEvent
    object OnExportClicked: GlueEvent
    data class OnSeekChanged(val positionMs: Long): GlueEvent
}