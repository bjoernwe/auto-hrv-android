package com.polar.polarsdkecghrdemo.domain.breathing

data class ExperimentConfig(
    val intervalSeconds: Float,
    val outToInRatioMean: Float,
    val outToInRatioStd: Float,
    val cycleLengthMean: Float,
    val cycleLengthStd: Float,
) {
    companion object {
        val DEFAULT = ExperimentConfig(
            intervalSeconds = 30f,
            outToInRatioMean = 1.5f,
            outToInRatioStd = 0.5f,
            cycleLengthMean = 9f,
            cycleLengthStd = 4f,
        )
    }
}
