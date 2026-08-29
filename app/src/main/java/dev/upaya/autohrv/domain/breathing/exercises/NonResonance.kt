package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness

data object NonResonance : Exercise {
    override val label = "Medium"

    val cycleLengthRange = 6..7

    override suspend fun run(business: BreathingBusiness) = holdCycleLengthRange(business, cycleLengthRange)
}
