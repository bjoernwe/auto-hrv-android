package com.polar.polarsdkecghrdemo.domain.breathing

data class ExperimentConfig(
    val intervalSeconds: Int,
    val outToInRatioMean: Float,
    val outToInRatioStd: Float,
    val cycleLengthMean: Float,
    val cycleLengthStd: Float,
) {
    fun defaultParams() = BreathingPattern(outToInRatioMean, cycleLengthMean)

    companion object {
        val DEFAULT = ExperimentConfig(
            intervalSeconds = 3,
            outToInRatioMean = 1.5f,
            outToInRatioStd = 0.5f,
            cycleLengthMean = 10f,
            cycleLengthStd = 4f,
        )
    }
}
