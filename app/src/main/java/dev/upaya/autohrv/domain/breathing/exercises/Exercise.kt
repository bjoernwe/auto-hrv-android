package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingConfig

data class Exercise(
    val label: String,
    val cycleLengthRange: IntRange,
) {
    companion object {
        val all: List<Exercise> =
            listOf(
                Exercise("Fast", 3..5),
                Exercise("Medium", 6..7),
                Exercise("Slow", 8..12),
                Exercise("Free", BreathingConfig.DEFAULT.maxCycleLengthRange),
            )
    }
}
