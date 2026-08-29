package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface Exercise {
    val label: String

    suspend fun run(business: BreathingBusiness)

    companion object {
        val all: List<Exercise> = listOf(FastResonance, NonResonance, SlowResonance, FreeRange)
    }
}

// Sets the target cycle-length range and holds it until it is changed manually (e.g. the user
// drags the slider), then returns. Restores the original range on cancellation (e.g. switching
// to another exercise) so a stopped exercise doesn't leave its range lingering.
suspend fun holdCycleLengthRange(
    business: BreathingBusiness,
    range: IntRange,
) {
    val originalRange = business.targetCycleLengthRange.value
    business.setTargetCycleLengthRange(range)
    try {
        business.targetCycleLengthRange.first { it != range }
    } catch (e: CancellationException) {
        business.setTargetCycleLengthRange(originalRange)
        throw e
    }
}
