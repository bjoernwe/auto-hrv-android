package com.polar.polarsdkecghrdemo.domain.breathing

data class ExperimentConfig(
    val experimentLengthSeconds: Int,
    val evaluationLengthSeconds: Int,
    val outToInRatioMean: Float,
    val outToInRatioStd: Float,
    val cycleLengthMean: Float,
    val cycleLengthStd: Float,
) {
    companion object {
        val DEFAULT = ExperimentConfig(
            experimentLengthSeconds = 30,
            evaluationLengthSeconds = 16+1, // 2*n + 1 is a good value for the power spectrum
            outToInRatioMean = 3f,
            outToInRatioStd = 0.5f,
            cycleLengthMean = 10f,
            cycleLengthStd = 2f,
        )
    }
}

fun ExperimentConfig.defaultParams() = BreathingPattern(outToInRatioMean, cycleLengthMean)
