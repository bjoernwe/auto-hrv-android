package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import dev.upaya.autohrv.domain.breathing.BreathingConfig

data object FreeRange : Exercise {

    override val label = "free range"

    override suspend fun run(business: BreathingBusiness) {
        business.setTargetCycleLengthRange(BreathingConfig.DEFAULT.maxCycleLengthRange)
    }
}
