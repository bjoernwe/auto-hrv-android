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
            outToInRatio = 2.5f,
            cycleLength = 10f,
            cycleLengthRange = 4f.rangeTo(12f),
        )
    }
}

fun BreathingConfig.defaultParams() = BreathingPattern(outToInRatio, cycleLength)
