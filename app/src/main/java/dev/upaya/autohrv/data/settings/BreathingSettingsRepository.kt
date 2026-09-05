package dev.upaya.autohrv.data.settings

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** User-adjustable breathing-pacer settings. In-memory for now; a future DataStore-backed persistence would live here. */
@Singleton
class BreathingSettingsRepository
    @Inject
    constructor() {
        private val defaults = BreathingConfig.DEFAULT

        val cycleLengthAllowedRange: IntRange = defaults.maxCycleLengthRange

        private val _targetInOutBias = MutableStateFlow(defaults.inOutBias)
        val targetInOutBias: StateFlow<Float> = _targetInOutBias

        fun setTargetInOutBias(bias: Float) {
            _targetInOutBias.value = bias
        }

        private val _targetCycleLengthRange = MutableStateFlow(defaults.maxCycleLengthRange)
        val targetCycleLengthRange: StateFlow<IntRange> = _targetCycleLengthRange

        fun setTargetCycleLengthRange(range: IntRange) {
            _targetCycleLengthRange.value = range.first.coerceIn(cycleLengthAllowedRange)..range.last.coerceIn(cycleLengthAllowedRange)
        }
    }
