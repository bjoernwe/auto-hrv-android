package dev.upaya.autohrv.domain.breathing

data class BreathingConfig(
    val evaluationLengthSeconds: Int,
    val outToInRatioMean: Float,
    val cycleLengthMean: Float,
) {
    companion object {
        val DEFAULT = BreathingConfig(
            evaluationLengthSeconds = 16+1, // 2^n + 1 is a good value for the power spectrum
            outToInRatioMean = 1.5f,
            cycleLengthMean = 10f,
        )
    }
}

fun BreathingConfig.defaultParams() = BreathingPattern(outToInRatioMean, cycleLengthMean)
