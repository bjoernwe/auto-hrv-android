package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import org.apache.commons.math3.linear.RealMatrix
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

private const val DIMENSIONS = 2
private const val MIN_RATIO = 0.2
private const val MIN_CYCLE = 2.0
private const val BOUND_STDS = 3.0
private const val MAX_ITERATIONS = 1000
private const val STOP_FITNESS = -1.0
private const val CONVERGENCE_REL = 1e-3
private const val CONVERGENCE_ABS = 1e-3

private fun DoubleArray.toBreathingPattern() = BreathingPattern(
    outToInRatio = this[0].toFloat().coerceAtLeast(MIN_RATIO.toFloat()),
    cycleLengthSeconds = this[1].toFloat().coerceAtLeast(MIN_CYCLE.toFloat()),
)

private fun RealMatrix.toBreathingPattern() = BreathingPattern(
    outToInRatio = getEntry(0, 0).toFloat().coerceAtLeast(MIN_RATIO.toFloat()),
    cycleLengthSeconds = getEntry(0, 1).toFloat().coerceAtLeast(MIN_CYCLE.toFloat()),
)

/**
 * Runs CMA-ES indefinitely, restarting around the best point after each convergence.
 * Wraps [objective] to also forward the current sampling-distribution mean on each evaluation.
 */
internal class CmaEsOptimizerUseCase @Inject constructor() {

    operator fun invoke(
        config: ExperimentConfig,
        objective: (BreathingPattern) -> Float,
        onSamplingMean: (BreathingPattern) -> Unit,
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

        val optimizer = CMAESOptimizer(
            MAX_ITERATIONS,
            STOP_FITNESS,
            true, // isActiveCMA
            0, // diagonalOnly
            1, // checkFeasableCount
            MersenneTwister(),
            true, // generateStatistics — exposes the sampling-distribution mean history
            SimpleValueChecker(CONVERGENCE_REL, CONVERGENCE_ABS),
        )

        var currentGuess = doubleArrayOf(
            config.outToInRatioMean.toDouble().coerceIn(lowerBounds[0], upperBounds[0]),
            config.cycleLengthMean.toDouble().coerceIn(lowerBounds[1], upperBounds[1]),
        )

        // Wrap the objective to also surface the sampling mean on each evaluation. CMA-ES appends
        // to the mean history on this same worker thread, so the read is race-free; redundant
        // repeats within a generation are conflated by the StateFlow downstream.
        val wrappedObjective = ObjectiveFunction { candidate ->
            val mean = optimizer.statisticsMeanHistory.lastOrNull()?.toBreathingPattern()
                ?: currentGuess.toBreathingPattern()
            onSamplingMean(mean)
            objective(candidate.toBreathingPattern()).toDouble()
        }

        try {
            while (true) {
                // Forget the previous run's means so the restart point shows until this run's
                // first generation completes.
                optimizer.statisticsMeanHistory.clear()
                val result = optimizer.optimize(
                    MaxEval(Int.MAX_VALUE),
                    wrappedObjective,
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
}
