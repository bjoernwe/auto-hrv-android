package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness

data object FastResonance : Exercise {
    override val label = "Fast"

    val cycleLengthRange = 3..5

    override suspend fun run(business: BreathingBusiness) = holdCycleLengthRange(business, cycleLengthRange)
}
