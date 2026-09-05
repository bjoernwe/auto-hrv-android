package dev.upaya.autohrv.domain.breathing.model

import dev.upaya.autohrv.domain.breathing.BreathingConfig

data class ExerciseBO(
    val label: String,
    val cycleLengthRange: IntRange,
) {
    companion object {
        val all: List<ExerciseBO> =
            listOf(
                ExerciseBO("Fast", 4..5),
                ExerciseBO("Med.", 6..7),
                ExerciseBO("Slow", 9..12),
                ExerciseBO("Free", BreathingConfig.DEFAULT.maxCycleLengthRange),
            )
    }
}
