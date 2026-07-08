package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.hours

data object NonResonance : Exercise {

    override val label = "non-resonance (6-7s)"

    val cycleLengthRange = 6..7

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
