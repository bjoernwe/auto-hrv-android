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
import org.apache.commons.math3.analysis.MultivariateFunction
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
        periodicity: () -> Float?,
    ): Flow<BreathingExperiment> = channelFlow {
        val asks = Channel<DoubleArray>(Channel.RENDEZVOUS)
        val tells = Channel<Double>(Channel.RENDEZVOUS)
        val experimentMs = (config.experimentLengthSeconds * 1000).toLong()
        val currentSamplingMean = AtomicReference(config.defaultParams())

        val objective = MultivariateFunction { candidate ->
            // ask: hand the candidate to the experiment loop
            if (asks.trySendBlocking(candidate.clone()).isClosed) {
                throw CancellationException("optimizer cancelled")
            }
            // tell: block until the experiment reports its (negated) periodicity
            runBlocking { tells.receive() }
        }

        launch(Dispatchers.Default) {
            optimizer(config, objective) { currentSamplingMean.set(it) }
        }

        try {
            for (candidate in asks) {
                send(BreathingExperiment(candidate.toBreathingPattern(), currentSamplingMean.get()))
                delay(experimentMs)
                // CMA-ES minimizes, so report the negated periodicity to maximize it.
                tells.send(-(periodicity() ?: 0f).toDouble())
            }
        } finally {
            asks.close()
            // Unblock the worker if it is waiting for a measurement that will never come.
            tells.close(CancellationException("experiment flow cancelled"))
        }
    }
}
