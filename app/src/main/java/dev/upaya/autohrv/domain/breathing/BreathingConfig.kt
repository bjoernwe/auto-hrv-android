package dev.upaya.autohrv.domain.breathing

data class BreathingConfig(
    val evaluationLengthSeconds: Int,
    val outToInRatio: Float,
    val cycleLength: Float,
    val cycleLengthRange: ClosedFloatingPointRange<Float>,
) {
    companion object {
        val DEFAULT = BreathingConfig(
            evaluationLengthSeconds = 16+1, // 2^n + 1 is a good value for the power spectrum
            outToInRatio = 1.5f,
            cycleLength = 10f,
            cycleLengthRange = 2f.rangeTo(12f),
        )
    }
}

fun BreathingConfig.defaultParams() = BreathingPattern(outToInRatio, cycleLength)
