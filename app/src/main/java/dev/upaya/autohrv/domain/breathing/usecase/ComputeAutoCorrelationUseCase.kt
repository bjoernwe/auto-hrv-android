package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.model.AutoCorrelationBO
import dev.upaya.autohrv.domain.signal.weightedAutoCorrelation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ComputeAutoCorrelationUseCase
    @Inject
    constructor() {

        /**
         * @param resampledRrsMs RR intervals on a uniform 1 Hz grid.
         * @param cycleLengthRange the lag range (seconds) searched for the breathing-cycle peak.
         * @param config supplies [BreathingConfig.acfMaxLagSeconds] (highest lag computed for the
         *   autocorrelation curve) and [BreathingConfig.acfHalfLifeSeconds] (recency half-life;
         *   `null` = uniform weights).
         */
        operator fun invoke(
            resampledRrsMs: Flow<List<Int>>,
            cycleLengthRange: IntRange,
            config: BreathingConfig,
        ): Flow<AutoCorrelationBO?> =
            resampledRrsMs.map { ts ->
                val acf = weightedAutoCorrelation(ts.map { it.toFloat() }, config.acfMaxLagSeconds, config.acfHalfLifeSeconds)
                acf?.let {
                    AutoCorrelationBO(
                        values = it,
                        peakLagSeconds = findBreathingCycleLength(it, cycleLengthRange),
                    )
                }
            }

        // Searches for the highest ACF peak within the allowed cycle-length range.
        // The RR stream is resampled to a uniform 1 Hz grid upstream, so lag == cycle length in seconds.
        internal fun findBreathingCycleLength(
            acf: List<Float>,
            range: IntRange,
        ): Float? {
            val minLag = range.first
            val maxLag = range.last.coerceAtMost(acf.size - 1)
            if (minLag > maxLag) return null
            val peakLag = (minLag..maxLag).maxByOrNull { acf[it] } ?: return null
            return peakLag.toFloat()
        }
    }
