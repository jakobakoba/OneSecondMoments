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

    fun onEvent(event: RecordEvent){
        when (event) {
            is RecordEvent.PermissionResult -> {
                _uiState.update { it.copy(hasPermissions = event.granted) }
            }
            is RecordEvent.ToggleRecording -> {
                if (_uiState.value.isRecording){
                    cameraManager.stopRecording()
                } else {
                    cameraManager.startRecording()
                    _uiState.update { it.copy(isRecording = true) }
                }
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