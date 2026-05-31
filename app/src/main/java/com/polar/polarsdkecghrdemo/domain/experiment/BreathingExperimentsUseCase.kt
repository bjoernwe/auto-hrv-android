package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.optim.InitialGuess
import org.apache.commons.math3.optim.MaxEval
import org.apache.commons.math3.optim.SimpleBounds
import org.apache.commons.math3.optim.SimpleValueChecker
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer
import org.apache.commons.math3.random.MersenneTwister
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max

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
internal class BreathingExperimentsUseCase @Inject constructor() {

    operator fun invoke(
        config: ExperimentConfig = ExperimentConfig.DEFAULT,
        periodicity: () -> Float?,
    ): Flow<BreathingPattern> = channelFlow {
        val asks = Channel<DoubleArray>(Channel.RENDEZVOUS)
        val tells = Channel<Double>(Channel.RENDEZVOUS)
        val experimentMs = (config.experimentLengthSeconds * 1000).toLong()

        launch(Dispatchers.Default) { runOptimizer(config, asks, tells) }

        try {
            for (candidate in asks) {
                send(candidate.toBreathingPattern())
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

    /**
     * Runs CMA-ES to completion (and restarts it around the best point found) until the
     * [asks] channel is closed. Each objective evaluation hands a candidate to the experiment
     * loop and blocks the worker thread until the corresponding periodicity is reported.
     */
    private fun runOptimizer(
        config: ExperimentConfig,
        asks: Channel<DoubleArray>,
        tells: Channel<Double>,
    ) {
        val lowerBounds = doubleArrayOf(
            max(MIN_RATIO, config.outToInRatioMean - BOUND_STDS * config.outToInRatioStd),
            max(MIN_CYCLE, config.cycleLengthMean - BOUND_STDS * config.cycleLengthStd),
        )
        val upperBounds = doubleArrayOf(
            config.outToInRatioMean + BOUND_STDS * config.outToInRatioStd,
            config.cycleLengthMean + BOUND_STDS * config.cycleLengthStd,
        )
        val initialSigma = doubleArrayOf(
            config.outToInRatioStd.toDouble(),
            config.cycleLengthStd.toDouble(),
        )
        val populationSize = 4 + floor(3.0 * ln(DIMENSIONS.toDouble())).toInt()

        val objective = ObjectiveFunction { candidate ->
            // ask: hand the candidate to the experiment loop
            if (asks.trySendBlocking(candidate.clone()).isClosed) {
                throw CancellationException("optimizer cancelled")
            }
            // tell: block until the experiment reports its (negated) periodicity
            runBlocking { tells.receive() }
        }

        val optimizer = CMAESOptimizer(
            MAX_ITERATIONS,
            STOP_FITNESS,
            true, // isActiveCMA
            0, // diagonalOnly
            1, // checkFeasableCount
            MersenneTwister(),
            false, // generateStatistics
            SimpleValueChecker(CONVERGENCE_REL, CONVERGENCE_ABS),
        )

        var currentGuess = doubleArrayOf(
            config.outToInRatioMean.toDouble().coerceIn(lowerBounds[0], upperBounds[0]),
            config.cycleLengthMean.toDouble().coerceIn(lowerBounds[1], upperBounds[1]),
        )
        try {
            while (true) {
                val result = optimizer.optimize(
                    MaxEval(Int.MAX_VALUE),
                    objective,
                    GoalType.MINIMIZE,
                    InitialGuess(currentGuess),
                    CMAESOptimizer.Sigma(initialSigma),
                    CMAESOptimizer.PopulationSize(populationSize),
                    SimpleBounds(lowerBounds, upperBounds),
                )
                // Restart around the best point found so the search keeps adapting.
                currentGuess = result.point
            }
        } catch (_: Exception) {
            // The asks/tells channels were closed because flow collection stopped.
        }
    }

    private fun DoubleArray.toBreathingPattern() = BreathingPattern(
        outToInRatio = this[0].toFloat().coerceAtLeast(MIN_RATIO.toFloat()),
        cycleLengthSeconds = this[1].toFloat().coerceAtLeast(MIN_CYCLE.toFloat()),
    )

    private companion object {
        const val DIMENSIONS = 2
        const val MIN_RATIO = 0.2
        const val MIN_CYCLE = 2.0
        const val BOUND_STDS = 2.0
        const val MAX_ITERATIONS = 1000
        const val STOP_FITNESS = -1.0
        const val CONVERGENCE_REL = 1e-3
        const val CONVERGENCE_ABS = 1e-3
    }
}
