package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.hours

data object SlowResonance : Exercise {

    override val label = "slow resonance (8-12s)"

    val cycleLengthRange = 8..12

    override suspend fun run(business: BreathingBusiness) {

        val originalRange = business.targetCycleLengthRange.value

        try {
            business.setTargetCycleLengthRange(cycleLengthRange)
            delay(24.hours)
        } finally {
            business.setTargetCycleLengthRange(originalRange)
        }

    }
}
