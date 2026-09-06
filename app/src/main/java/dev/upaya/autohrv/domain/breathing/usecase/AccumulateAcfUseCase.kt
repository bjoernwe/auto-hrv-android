package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.signal.decayPerEmission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

/**
 * Session-accumulated ACF sums: every emitted ACF summed element-wise (see [accumulate]), decaying
 * older emissions per [BreathingConfig.acfHistogramHalfLifeSeconds] so the sums track the recent
 * session rather than accumulating indefinitely. `null` values (no ACF yet) are ignored, and
 * consecutive identical ACFs are deduplicated so an unchanged RR window is not double-counted.
 * Emissions arrive roughly once per second (after dedup), so the half-life in seconds roughly
 * matches the half-life in emissions.
 */
internal class AccumulateAcfUseCase
    @Inject
    constructor() {

        operator fun invoke(
            acf: Flow<List<Float>?>,
            config: BreathingConfig,
        ): Flow<List<Float>> {
            val decay = decayPerEmission(config.acfHistogramHalfLifeSeconds)
            return acf
                .mapNotNull { it }
                .distinctUntilChanged()
                .scan(emptyList()) { acc, values -> accumulate(acc, values, decay) }
        }

        /**
         * Element-wise running sum of successive ACF emissions, decaying the prior accumulator by
         * [decay] before adding the new value (`decay = 1f` is a plain running sum). A size mismatch
         * (including an empty accumulator on the first emission) restarts the sum from [acf], so the
         * accumulator always tracks the current ACF length.
         */
        internal fun accumulate(
            acc: List<Float>,
            acf: List<Float>,
            decay: Float,
        ): List<Float> {
            if (acc.size != acf.size) return acf
            return acc.zip(acf) { a, b -> a * decay + b }
        }
    }
