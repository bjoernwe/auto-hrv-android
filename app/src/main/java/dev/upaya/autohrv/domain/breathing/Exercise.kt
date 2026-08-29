package dev.upaya.autohrv.domain.breathing

data class Exercise(
    val label: String,
    val cycleLengthRange: IntRange,
) {
    companion object {
        val all: List<Exercise> =
            listOf(
                Exercise("Fast", 4..5),
                Exercise("Medium", 6..7),
                Exercise("Slow", 9..12),
                Exercise("Free", BreathingConfig.DEFAULT.maxCycleLengthRange),
            )
    }
}
