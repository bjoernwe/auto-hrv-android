package com.polar.polarsdkecghrdemo.domain.breathing

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.apache.commons.math3.distribution.NormalDistribution
import javax.inject.Inject

class GenerateBreathingParamsUseCase @Inject constructor() {

    operator fun invoke(
        intervalSeconds: Float,
        outToInRatioMean: Float,
        outToInRatioStd: Float,
        cycleLengthMean: Float,
        cycleLengthStd: Float,
    ): Flow<BreathingParams> = flow {
        val ratioDist = NormalDistribution(outToInRatioMean.toDouble(), outToInRatioStd.toDouble())
        val lengthDist = NormalDistribution(cycleLengthMean.toDouble(), cycleLengthStd.toDouble())
        val intervalMs = (intervalSeconds * 1000).toLong()

        while (currentCoroutineContext().isActive) {
            emit(
                BreathingParams(
                    outToInRatio = ratioDist.sample().toFloat().coerceAtLeast(0.1f),
                    cycleLengthSeconds = lengthDist.sample().toFloat().coerceAtLeast(2f),
                ),
            )
            delay(intervalMs)
        }
    }
}
