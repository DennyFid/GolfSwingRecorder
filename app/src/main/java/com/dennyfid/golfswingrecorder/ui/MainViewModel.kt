package com.dennyfid.golfswingrecorder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dennyfid.golfswingrecorder.camera.CameraManager
import com.dennyfid.golfswingrecorder.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.util.Log
import androidx.camera.video.VideoRecordEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@HiltViewModel
class MainViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private var motionStartTime: Long = 0
    private var lastMotionTime: Long = 0
    private var recordingJob: Job? = null

    private var swingThreshold = 5000.0
    private var preRollSec = 1

    private val MOTION_PERSIST_MS = 200L
    private val NO_MOTION_STOP_MS = 2000L
    private val MAX_RECORD_MS = 10000L

    init {
        viewModelScope.launch {
            settingsRepository.motionThreshold.collect {
                swingThreshold = it
            }
        }
        viewModelScope.launch {
            settingsRepository.preRollSec.collect {
                preRollSec = it
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Waiting : UiState()
        object Recording : UiState()
        object Saving : UiState()
    }

    fun startSession() {
        if (_uiState.value == UiState.Idle) {
            _uiState.value = UiState.Waiting
        } else {
            stopSession()
        }
    }

    private fun stopSession() {
        if (_uiState.value == UiState.Recording) {
            cameraManager.stopRecording()
        }
        _uiState.value = UiState.Idle
        recordingJob?.cancel()
    }

    fun onMotionDetected(score: Double) {
        val currentTime = System.currentTimeMillis()

        if (score > 100) { // Log minor motion to see activity
            Log.v("Motion", "Motion Score: $score (Threshold: $swingThreshold)")
        }

        when (_uiState.value) {
            UiState.Waiting -> {
                if (score > swingThreshold) {
                    if (motionStartTime == 0L) {
                        motionStartTime = currentTime
                        Log.d("Motion", "Motion started...")
                    } else if (currentTime - motionStartTime >= MOTION_PERSIST_MS) {
                        Log.i("Motion", "Motion persistent for ${MOTION_PERSIST_MS}ms. STARTING RECORDING.")
                        startRecording()
                    }
                } else {
                    motionStartTime = 0
                }
            }
            UiState.Recording -> {
                if (score > swingThreshold) {
                    lastMotionTime = currentTime
                } else if (currentTime - lastMotionTime >= NO_MOTION_STOP_MS) {
                    Log.i("Motion", "No motion for ${NO_MOTION_STOP_MS}ms. STOPPING RECORDING.")
                    stopRecording()
                }
            }
            else -> {}
        }
    }

    private fun startRecording() {
        _uiState.value = UiState.Recording
        lastMotionTime = System.currentTimeMillis()
        
        cameraManager.startRecording { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (event.hasError()) {
                    Log.e("MainViewModel", "Recording error: ${event.error}")
                } else {
                    Log.i("MainViewModel", "Recording saved: ${event.outputResults.outputUri}")
                }
                _uiState.value = UiState.Waiting
            }
        }

        recordingJob = viewModelScope.launch {
            delay(MAX_RECORD_MS)
            if (_uiState.value == UiState.Recording) {
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        if (_uiState.value == UiState.Recording) {
            _uiState.value = UiState.Saving
            cameraManager.stopRecording()
            recordingJob?.cancel()
        }
    }
}
