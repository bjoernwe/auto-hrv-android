package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.defaultParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

data class BreathingExperiment(
    val candidate: BreathingPattern,
    val samplingMean: BreathingPattern,
)

/**
 * Optimizes the breathing parameters (out-to-in ratio and cycle length) with CMA-ES from
 * Apache Commons Math, using the observed [TimeSeriesStats.periodicity] as the objective to
 * maximize.
 *
 * CMA-ES is a synchronous, population-based optimizer, while evaluating a candidate requires
 * pacing the breathing for [ExperimentConfig.experimentLengthSeconds] and then reading the
 * resulting periodicity. The two worlds are bridged with an ask/tell handshake: the optimizer
 * runs on a worker thread and blocks on rendezvous channels, while the emitted flow drives the
 * experiment timing and feeds the measurement back.
 */
internal class BreathingExperimentsUseCase @Inject constructor(
    private val optimizer: CmaEsOptimizerUseCase,
) {

    operator fun invoke(
        config: ExperimentConfig = ExperimentConfig.DEFAULT,
        objective: () -> Float,
    ): Flow<BreathingExperiment> = channelFlow {
        val asks = Channel<BreathingPattern>(Channel.RENDEZVOUS)
        val tells = Channel<Double>(Channel.RENDEZVOUS)
        val experimentMs = (config.experimentLengthSeconds * 1000).toLong()
        val currentSamplingMean = AtomicReference(config.defaultParams())

        val optimizerObjective: (BreathingPattern) -> Double = { candidate ->
            val channelResult = asks.trySendBlocking(candidate)
            if (channelResult.isClosed) { throw CancellationException("optimizer cancelled") }
            runBlocking { tells.receive() }
        }

        launch(Dispatchers.Default) {
            optimizer(config, optimizerObjective) { currentSamplingMean.set(it) }
        }

        try {
            for (candidate in asks) {
                send(BreathingExperiment(candidate, currentSamplingMean.get()))
                delay(experimentMs)
                tells.send(objective().toDouble())
            }
        } finally {
            asks.close()
            // Unblock the worker if it is waiting for a measurement that will never come.
            tells.close(CancellationException("experiment flow cancelled"))
        }
    }
}
