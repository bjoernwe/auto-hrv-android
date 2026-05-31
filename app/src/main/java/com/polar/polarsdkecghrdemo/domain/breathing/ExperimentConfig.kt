package com.polar.polarsdkecghrdemo.domain.breathing

data class ExperimentConfig(
    val experimentLengthSeconds: Int,
    val evaluationLengthSeconds: Int,
    val outToInRatioMean: Float,
    val outToInRatioStd: Float,
    val cycleLengthMean: Float,
    val cycleLengthStd: Float,
) {
    fun defaultParams() = BreathingPattern(outToInRatioMean, cycleLengthMean)

    companion object {
        val DEFAULT = ExperimentConfig(
            experimentLengthSeconds = 20,
            evaluationLengthSeconds = 16+1,
            outToInRatioMean = 1.5f,
            outToInRatioStd = 0.5f,
            cycleLengthMean = 10f,
            cycleLengthStd = 4f,
        )
    }
}
