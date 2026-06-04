package dev.upaya.autohrv.ui.breathing

import androidx.lifecycle.ViewModel
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingState
import dev.upaya.autohrv.domain.breathing.BreathingCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    private val coordinator: BreathingCoordinator,
) : ViewModel() {
    val breathingState: StateFlow<BreathingState> = coordinator.currentBreathingState
    val currentPattern: StateFlow<BreathingPattern> = coordinator.currentBreathingPattern
    val targetOutToInRatio: StateFlow<Float> = coordinator.targetOutToInRatio
    val targetCycleLengthRange: StateFlow<ClosedFloatingPointRange<Float>> = coordinator.targetCycleLengthRange
    val cycleLengthAllowedRange: ClosedFloatingPointRange<Float> = coordinator.cycleLengthAllowedRange

    fun setTargetOutToInRatio(ratio: Float) {
        coordinator.setTargetOutToInRatio(ratio)
    }

    fun setTargetCycleLengthRange(range: ClosedFloatingPointRange<Float>) {
        coordinator.setTargetCycleLengthRange(range)
    }
}
