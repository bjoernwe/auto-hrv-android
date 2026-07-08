package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness

data object FastResonance : Exercise {
    override val label = "Fast resonance (4-5s)"
    val cycleLengthRange = 4..5

    override suspend fun run(business: BreathingBusiness) {
        business.setTargetCycleLengthRange(cycleLengthRange)
    }
}
