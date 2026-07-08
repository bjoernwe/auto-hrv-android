package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness

sealed interface Exercise {
    val label: String

    suspend fun run(business: BreathingBusiness)

    companion object {
        val all: List<Exercise> = listOf(FreeRange, SlowResonance, NonResonance, FastResonance, Sweep)
    }
}
