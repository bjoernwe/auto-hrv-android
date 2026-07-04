package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.sqrt

data class ResampledRrsStats(
    val autoCorrelation: List<Float>?,
    val autoCorrelationPeak: Float?,
)

data class BeatRrsStats(
    val sdrr: Float?,
    val rmssd: Float?,
)

data class TimeSeriesStats(
    val resampledRrsStats: ResampledRrsStats?,
    val beatRrsStats: BeatRrsStats?,
)

internal class TimeSeriesStatsUseCase
    @Inject
    constructor() {

        /**
         * @param resampledRrsMs RR intervals on a uniform 1 Hz grid — basis for the ACF stats.
         * @param beatRrsMs beat-indexed RR intervals — basis for [BeatRrsStats.sdrr] and
         *   [BeatRrsStats.rmssd], which would be biased by the zero-order-hold resampling of
         *   [resampledRrsMs].
         * @param cycleLengthRange the lag range (seconds) searched for the breathing-cycle peak.
         * @param maxLagSeconds highest lag computed for the autocorrelation curve.
         * @param halfLifeSeconds recency half-life for the autocorrelation; `null` = uniform weights.
         */
        operator fun invoke(
            resampledRrsMs: Flow<List<Int>>,
            beatRrsMs: Flow<List<Int>>,
            cycleLengthRange: ClosedFloatingPointRange<Float>,
            maxLagSeconds: Int,
            halfLifeSeconds: Float?,
        ): Flow<TimeSeriesStats> {
            val resampledStats =
                resampledRrsMs.map { ts ->
                    val acf = weightedAutoCorrelation(ts.map { it.toFloat() }, maxLagSeconds, halfLifeSeconds)
                    ResampledRrsStats(
                        autoCorrelation = acf,
                        autoCorrelationPeak = acf?.let { findBreathingCycleLength(it, cycleLengthRange) },
                    )
                }
            val beatStats = beatRrsMs.map { BeatRrsStats(sdrr = computeStdDev(it), rmssd = computeRmssd(it)) }
            return combine(resampledStats, beatStats) { resampled, beat ->
                TimeSeriesStats(resampledRrsStats = resampled, beatRrsStats = beat)
            }
        }

        // Searches for the highest ACF peak within the allowed cycle-length range.
        // The RR stream is resampled to a uniform 1 Hz grid upstream, so lag == cycle length in seconds.
        internal fun findBreathingCycleLength(
            acf: List<Float>,
            range: ClosedFloatingPointRange<Float>,
        ): Float? {
            val minLag = range.start.toInt()
            val maxLag = range.endInclusive.toInt().coerceAtMost(acf.size - 1)
            if (minLag > maxLag) return null
            val peakLag = (minLag..maxLag).maxByOrNull { acf[it] } ?: return null
            return peakLag.toFloat()
        }

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
