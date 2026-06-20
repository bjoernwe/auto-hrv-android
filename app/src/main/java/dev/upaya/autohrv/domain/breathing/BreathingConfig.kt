package dev.upaya.autohrv.domain.breathing

data class BreathingConfig(
    val evaluationLengthSeconds: Int,
    val outToInRatio: Float,
    val cycleLength: Float,
    val cycleLengthRange: ClosedFloatingPointRange<Float>,
) {
    companion object {
        val DEFAULT = BreathingConfig(
            evaluationLengthSeconds = 32+1, // 2^n + 1 is a good value for the power spectrum
            outToInRatio = 1f,
            cycleLength = 8f,
            cycleLengthRange = 5f.rangeTo(11f),
        )
    }
}

fun BreathingConfig.defaultParams() = BreathingPattern(outToInRatio, cycleLength)
