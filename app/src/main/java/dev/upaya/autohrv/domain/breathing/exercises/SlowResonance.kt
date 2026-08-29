package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness

data object SlowResonance : Exercise {
    override val label = "Slow"

    val cycleLengthRange = 8..12

    override suspend fun run(business: BreathingBusiness) = holdCycleLengthRange(business, cycleLengthRange)
}
