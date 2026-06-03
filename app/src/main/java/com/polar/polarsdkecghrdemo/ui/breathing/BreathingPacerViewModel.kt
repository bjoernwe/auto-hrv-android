package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import com.polar.polarsdkecghrdemo.domain.experiment.ExperimentCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    private val coordinator: ExperimentCoordinator,
) : ViewModel() {
    val breathingState: StateFlow<BreathingState> = coordinator.currentBreathingState
    val currentPattern: StateFlow<BreathingPattern> = coordinator.currentBreathingPattern
    val targetOutToInRatio: StateFlow<Float> = coordinator.targetOutToInRatio

    fun setTargetOutToInRatio(ratio: Float) {
        coordinator.setTargetOutToInRatio(ratio)
    }
}
