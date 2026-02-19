package com.bor96dev.glue.presentation.state

import android.net.Uri
import com.bor96dev.database.MomentEntity
import java.util.UUID

data class GlueState (
    val title: String = "",
    val videoMoments: List<MomentEntity> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val totalDurationMs: Long = 0L,
    val currentTimeMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isExporting : Boolean = false,
    val videoVolume: Float = 0.7f,
    val musicVolume: Float = 1.0f,
    val error: String? = null
    )

data class AudioTrack (
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val startInTimelineMs: Long = 0,
    val endInTimelineMs: Long = 0,
    val trimStartMs: Long = 0,
    val volume: Float = 1.0f
)