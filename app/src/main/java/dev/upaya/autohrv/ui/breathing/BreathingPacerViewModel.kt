package dev.upaya.autohrv.ui.breathing

import androidx.lifecycle.ViewModel
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingState
import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    private val breathingBusiness: BreathingBusiness,
) : ViewModel() {
    val breathingState: StateFlow<BreathingState> = breathingBusiness.currentBreathingState
    val currentPattern: StateFlow<BreathingPattern> = breathingBusiness.currentBreathingPattern
    val targetOutToInRatio: StateFlow<Float> = breathingBusiness.targetOutToInRatio
    val targetCycleLengthRange: StateFlow<ClosedFloatingPointRange<Float>> = breathingBusiness.targetCycleLengthRange
    val cycleLengthAllowedRange: ClosedFloatingPointRange<Float> = breathingBusiness.cycleLengthAllowedRange

    fun setTargetOutToInRatio(ratio: Float) {
        breathingBusiness.setTargetOutToInRatio(ratio)
    }

    fun setTargetCycleLengthRange(range: ClosedFloatingPointRange<Float>) {
        breathingBusiness.setTargetCycleLengthRange(range)
    }
}
