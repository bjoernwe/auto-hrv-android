package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.apache.commons.math3.distribution.NormalDistribution
import javax.inject.Inject

internal class BreathingExperimentsUseCase @Inject constructor() {

    operator fun invoke(config: ExperimentConfig = ExperimentConfig.DEFAULT): Flow<BreathingPattern> =
        flow {
            val ratioDist = NormalDistribution(
                config.outToInRatioMean.toDouble(),
                config.outToInRatioStd.toDouble()
            )
            val lengthDist = NormalDistribution(
                config.cycleLengthMean.toDouble(),
                config.cycleLengthStd.toDouble()
            )
            val intervalMs = (config.intervalSeconds * 1000).toLong()

            while (currentCoroutineContext().isActive) {
                emit(
                    BreathingPattern(
                        outToInRatio = ratioDist.sample().toFloat().coerceAtLeast(0.1f),
                        cycleLengthSeconds = lengthDist.sample().toFloat().coerceAtLeast(2f),
                    ),
                )
                delay(intervalMs)
            }
        }
}
