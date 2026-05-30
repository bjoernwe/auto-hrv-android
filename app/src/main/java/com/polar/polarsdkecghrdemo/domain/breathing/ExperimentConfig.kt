package com.polar.polarsdkecghrdemo.domain.breathing

data class ExperimentConfig(
    val intervalSeconds: Float,
    val outToInRatioMean: Float,
    val outToInRatioStd: Float,
    val cycleLengthMean: Float,
    val cycleLengthStd: Float,
) {
    fun defaultParams() = BreathingParams(outToInRatioMean, cycleLengthMean)

    companion object {
        val DEFAULT = ExperimentConfig(
            intervalSeconds = 30f,
            outToInRatioMean = 1.5f,
            outToInRatioStd = 0.5f,
            cycleLengthMean = 10f,
            cycleLengthStd = 4f,
        )
    }
}
