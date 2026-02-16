package com.bor96dev.record.presentation

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bor96dev.record.domain.CameraManager
import com.bor96dev.record.domain.CameraResult
import com.bor96dev.record.domain.RecordingStatus
import com.bor96dev.record.presentation.event.RecordEvent
import com.bor96dev.record.presentation.state.RecordState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val cameraManager: CameraManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordState())
    val uiState = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var recordingStartDate: Long? = null

    init {
        val preview = Preview.Builder().build()
        _uiState.update { it.copy(videoPreview = preview) }

        viewModelScope.launch {
            cameraManager.results.collect { result ->
                when (result) {
                    is CameraResult.VideoSaved -> {
                        val date = recordingStartDate ?: System.currentTimeMillis()
                        _uiState.update {
                            it.copy(
                                lastRecordedUri = result.uri,
                                recordedDate = date,
                                isProcessing = true
                            )
                        }
                    }

                    is CameraResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                isRecording = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            cameraManager.recordingStatus.collect { status ->
                if (status is RecordingStatus.Started) {
                    startTimers()
                }
            }
        }
    }

    private fun startRecording() {
        if (_uiState.value.isProcessing || _uiState.value.isRecording) return
        _uiState.update {
            it.copy(
                isRecording = true,
                canStop = false,
                error = null
            )
        }
        recordingStartDate = System.currentTimeMillis()
        cameraManager.startRecording()
    }

    private fun startTimers() {
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            delay(1000)
            _uiState.update { it.copy(canStop = true) }
            delay(14000)
            if (_uiState.value.isRecording) {
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        _uiState.update {
            it.copy(
                isRecording = false,
                canStop = false,
                isProcessing = true
            )
        }
        recordingJob?.cancel()
        recordingJob = null
        cameraManager.stopRecording()
    }

    fun onEvent(event: RecordEvent) {
        when (event) {
            is RecordEvent.PermissionResult -> {
                _uiState.update { it.copy(hasPermissions = event.granted) }
            }

            is RecordEvent.ToggleRecording -> {
                val currentState = _uiState.value
                if (currentState.isProcessing || currentState.lastRecordedUri != null) return

                if (currentState.isRecording) {
                    if (currentState.canStop) {
                        stopRecording()
                    }
                } else {
                    startRecording()
                }
            }

            is RecordEvent.OrientationChanged -> {
                _uiState.update { it.copy(isLandscape = event.isLandscape) }
                cameraManager.setTargetRotation(event.rotation)
            }

            RecordEvent.OnNavigationDone -> {
                _uiState.update {
                    it.copy(
                        lastRecordedUri = null,
                        recordedDate = null,
                        isProcessing = false,
                        isRecording = false
                    )
                }
            }
        }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            val preview = _uiState.value.videoPreview ?: return@launch
            cameraManager.bindToLifecycle(lifecycleOwner, preview)
        }
    }
}