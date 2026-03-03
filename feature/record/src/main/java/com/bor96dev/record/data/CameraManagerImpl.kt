package com.bor96dev.record.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bor96dev.record.domain.CameraManager
import com.bor96dev.record.domain.CameraResult
import com.bor96dev.record.domain.RecordingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class CameraManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraManager {
    private val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()
    override val videoCapture = VideoCapture.withOutput(recorder)
    private var currentRecording: Recording? = null
    private val _results = MutableSharedFlow<CameraResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val results = _results.asSharedFlow()
    private val _recordingStatus = MutableSharedFlow<RecordingStatus>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val recordingStatus = _recordingStatus.asSharedFlow()

    @SuppressLint("MissingPermission")
    override fun startRecording() {
        if (currentRecording != null) return
        val name = "OneSecond-" + SimpleDateFormat(
            "yyyyMMdd-HHmmss",
            Locale.US
        ).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/OneSecondMoments")
            }
        }
        val outputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        currentRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Start){
                    _recordingStatus.tryEmit(RecordingStatus.Started)
                }
                if (event is VideoRecordEvent.Finalize) {
                    currentRecording = null
                    if (event.hasError()) {
                        _results.tryEmit(CameraResult.Error("Recording error code: ${event.error}"))
                    } else {
                        _results.tryEmit(CameraResult.VideoSaved(event.outputResults.outputUri))
                    }
                }
            }
    }

    override fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
    }

    override fun setTargetRotation(rotation: Int) {
        videoCapture.targetRotation = rotation
    }

    override suspend fun unbind() {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        cameraProvider.unbindAll()
    }

    override suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        vararg useCases: UseCase
    ) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            *useCases,
            videoCapture
        )
    }
}
