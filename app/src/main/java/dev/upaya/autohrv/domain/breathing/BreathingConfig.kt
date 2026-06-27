package dev.upaya.autohrv.domain.breathing

data class BreathingConfig(
    val evaluationLengthSeconds: Int,
    val outToInRatio: Float,
    val initialCycleLength: Float,
    val initialCycleLengthRange: ClosedFloatingPointRange<Float>,
    val maxCycleLengthRange: ClosedFloatingPointRange<Float>,
    val resonancePeakToleranceSeconds: Float,
    val resonanceMinPeakValue: Float,
) {
    companion object {
        val DEFAULT = BreathingConfig(
            evaluationLengthSeconds = 32+1, // 2^n + 1 is a good value for the power spectrum
            outToInRatio = 1f,
            initialCycleLength = 8f,
            initialCycleLengthRange = 6f..10f,
            maxCycleLengthRange = 4f..20f,
            resonancePeakToleranceSeconds = 1.5f,
            resonanceMinPeakValue = 0.35f,
        )
    }
}

fun BreathingConfig.defaultPattern() = BreathingPattern(outToInRatio, initialCycleLength)
