package com.dennyfid.golfswingrecorder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dennyfid.golfswingrecorder.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val motionThreshold = settingsRepository.motionThreshold.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 5000.0
    )

    val preRollSec = settingsRepository.preRollSec.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 1
    )

    fun setMotionThreshold(value: Double) {
        viewModelScope.launch {
            settingsRepository.setMotionThreshold(value)
        }
    }

    fun setPreRollSec(value: Int) {
        viewModelScope.launch {
            settingsRepository.setPreRollSec(value)
        }
    }
}
