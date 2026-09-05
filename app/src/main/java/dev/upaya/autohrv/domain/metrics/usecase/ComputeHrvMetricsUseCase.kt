package dev.upaya.autohrv.domain.metrics.usecase

import dev.upaya.autohrv.domain.metrics.model.HrvMetricsBO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.sqrt

internal class ComputeHrvMetricsUseCase
    @Inject
    constructor() {

        /**
         * @param beatRrsMs beat-indexed RR intervals (true NN intervals) — the basis for
         *   [HrvMetricsBO.sdrr] and [HrvMetricsBO.rmssd], which would be biased by the zero-order-hold
         *   resampling of a uniform-grid signal.
         */
        operator fun invoke(beatRrsMs: Flow<List<Int>>): Flow<HrvMetricsBO> =
            beatRrsMs.map { HrvMetricsBO(sdrr = computeStdDev(it), rmssd = computeRmssd(it)) }

        // Sample standard deviation (Bessel-corrected, divides by N-1) — matches the SDNN convention.
        private fun computeStdDev(ts: List<Int>): Float? {
            if (ts.size < 2) return null
            val mean = ts.average().toFloat()
            val sumSquares =
                ts
                    .sumOf {
                        val d = it - mean
                        (d * d).toDouble()
                    }.toFloat()
            return sqrt(sumSquares / (ts.size - 1))
        }

        // Root mean square of successive differences between adjacent RR intervals.
        private fun computeRmssd(ts: List<Int>): Float? {
            if (ts.size < 2) return null
            val meanSquaredDiff =
                ts
                    .zipWithNext { a, b ->
                        val d = (b - a).toDouble()
                        d * d
                    }.average()
            return sqrt(meanSquaredDiff).toFloat()
        }
    }
