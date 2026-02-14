package com.bor96dev.record.presentation

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bor96dev.record.domain.CameraManager
import com.bor96dev.record.domain.CameraResult
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
): ViewModel() {
    private val _uiState = MutableStateFlow(RecordState())
    val uiState = _uiState.asStateFlow()

    private var recordingJob: Job? = null

    init {
        val preview = Preview.Builder().build()
        _uiState.update{it.copy(videoPreview = preview)}

        viewModelScope.launch {
            cameraManager.results.collect {result ->
                when(result){
                    is CameraResult.VideoSaved -> {
                       _uiState.update {it.copy(
                           isRecording = false,
                           lastRecordedUri = result.uri
                       )}
                    }
                    is CameraResult.Error -> {
                        _uiState.update {it.copy(
                            isRecording = false,
                            error = result.message
                        )}
                    }
                }
            }
        }
    }

    private fun startRecording() {
        cameraManager.startRecording()
        val startTime = System.currentTimeMillis()
        _uiState.update {it.copy(
            isRecording = true,
            recordingStartTime = startTime,
            canStop = false
        )}

        recordingJob = viewModelScope.launch {
            delay(1000)
            _uiState.update{it.copy(canStop = true)}
            delay(14000)
            if (_uiState.value.isRecording){
                stopRecording()
            }
        }
    }

    private fun stopRecording(){
        val startTime = _uiState.value.recordingStartTime ?: 0L
        if (System.currentTimeMillis() - startTime < 1000) return
        recordingJob?.cancel()
        recordingJob = null
        cameraManager.stopRecording()
        _uiState.update{it.copy(isRecording = false, canStop = false)}
    }

    fun onEvent(event: RecordEvent){
        when (event) {
            is RecordEvent.PermissionResult -> {
                _uiState.update { it.copy(hasPermissions = event.granted) }
            }
            is RecordEvent.ToggleRecording -> {
                if (_uiState.value.isRecording){
                    if (_uiState.value.canStop){
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
                _uiState.update { it.copy(lastRecordedUri = null) }
            }
        }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner){
        viewModelScope.launch{
            val preview = _uiState.value.videoPreview ?: return@launch
            cameraManager.bindToLifecycle(lifecycleOwner, preview)
        }
    }
}