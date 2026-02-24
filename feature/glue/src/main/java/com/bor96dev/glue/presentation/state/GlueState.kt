package com.bor96dev.glue.presentation.state

import android.net.Uri
import com.bor96dev.database.MomentEntity
import java.util.UUID

data class GlueState(
    val title: String = "",
    val videoMoments: List<MomentEntity> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val totalDurationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isMerging: Boolean = false,
    val mergeProgress: Float = 0f,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val error: String? = null
)

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val fileDurationMs: Long,
    val startInTimelineMs: Long = 0,
    val endInTimelineMs: Long = 0,
    val trimStartMs: Long = 0
)