package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.hours

data object FastResonance : Exercise {

    override val label = "fast resonance (4-5s)"

    val cycleLengthRange = 4..5

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
