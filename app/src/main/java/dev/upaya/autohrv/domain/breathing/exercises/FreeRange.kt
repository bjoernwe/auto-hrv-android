package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import dev.upaya.autohrv.domain.breathing.BreathingConfig

data object FreeRange : Exercise {

    override val label = "Free"

    override suspend fun run(business: BreathingBusiness) = holdCycleLengthRange(business, BreathingConfig.DEFAULT.maxCycleLengthRange)
}
