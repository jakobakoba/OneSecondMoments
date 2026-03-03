package com.bor96dev.record.domain

import android.net.Uri
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import androidx.camera.core.UseCase

interface CameraManager {
    val videoCapture: VideoCapture<Recorder>
    val results: Flow<CameraResult>
    val recordingStatus: Flow<RecordingStatus>
    fun startRecording()
    fun stopRecording()
    fun setTargetRotation(rotation: Int)
    suspend fun unbind()
    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        vararg useCases: UseCase
    )
}

sealed interface CameraResult {
    data class VideoSaved(val uri: Uri) : CameraResult
    data class Error(val message: String): CameraResult
}

sealed interface RecordingStatus {
    data object Started: RecordingStatus
}
