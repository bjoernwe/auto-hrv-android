package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data object SlowResonance : Exercise {
    override val label = "slow resonance (8-12s)"

    val cycleLengthRange = 8..12

    override suspend fun run(business: BreathingBusiness) {
        val originalRange = business.targetCycleLengthRange.value
        business.setTargetCycleLengthRange(cycleLengthRange)
        try {
            // Hold this range until it is changed manually (e.g. the user drags the slider), then stop.
            business.targetCycleLengthRange.first { it != cycleLengthRange }
        } catch (e: CancellationException) {
            // Stopped via toggle-off/switch: put back the range we replaced.
            business.setTargetCycleLengthRange(originalRange)
            throw e
        }
    }
}
