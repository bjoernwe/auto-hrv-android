package dev.upaya.autohrv.domain.breathing

data class BreathingConfig(
    val fftSize: Int,
    val outToInRatio: Float,
    val initialCycleLength: Float,
    val initialCycleLengthRange: ClosedFloatingPointRange<Float>,
    val maxCycleLengthRange: ClosedFloatingPointRange<Float>,
    val targetCycleLengthSmoothingWindow: Int,
    val resonancePeakToleranceSeconds: Float,
    val resonanceMinPeakValue: Float,
    val windowLength: Int,
) {
    companion object {
        val DEFAULT = BreathingConfig(
            fftSize = 32+1, // 2^n + 1
            outToInRatio = 1f,
            initialCycleLength = 8f,
            initialCycleLengthRange = 6f..10f,
            maxCycleLengthRange = 4f..20f,
            targetCycleLengthSmoothingWindow = 40,
            resonancePeakToleranceSeconds = 1.5f,
            resonanceMinPeakValue = 0.35f,
            windowLength = 20,
        )
    }
}

fun BreathingConfig.defaultPattern() = BreathingPattern(outToInRatio, initialCycleLength)
