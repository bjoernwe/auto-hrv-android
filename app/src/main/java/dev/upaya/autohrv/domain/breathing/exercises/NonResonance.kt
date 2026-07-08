package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness

data object NonResonance : Exercise {
    override val label = "Non-resonance (6-7s)"
    val cycleLengthRange = 6..7

    override suspend fun run(business: BreathingBusiness) {
        business.setTargetCycleLengthRange(cycleLengthRange)
    }
}
