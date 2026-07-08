package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness

data object SlowResonance : Exercise {
    override val label = "Slow resonance (8-12s)"
    val cycleLengthRange = 8..12

    override suspend fun run(business: BreathingBusiness) {
        business.setTargetCycleLengthRange(cycleLengthRange)
    }
}
