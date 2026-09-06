package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.model.ACF_PCA_FIRST_LAG
import dev.upaya.autohrv.domain.breathing.model.AcfMomentsBO
import dev.upaya.autohrv.domain.signal.decayPerEmission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

/**
 * Session-accumulated first and second moments of the ACF curves (see [accumulate]), decaying older
 * emissions per [BreathingConfig.acfPcaHalfLifeSeconds] so the covariance derived from them tracks
 * the recent session. Follows [AccumulateAcfUseCase]: `null` curves are ignored and consecutive
 * identical curves are deduplicated, so an unchanged RR window is not double-counted. Emissions
 * arrive roughly once per second, so the half-life in seconds roughly matches the half-life in
 * emissions.
 */
internal class AccumulateAcfMomentsUseCase
    @Inject
    constructor() {

        operator fun invoke(
            acf: Flow<List<Float>?>,
            config: BreathingConfig,
        ): Flow<AcfMomentsBO> {
            val decay = decayPerEmission(config.acfPcaHalfLifeSeconds)
            return acf
                .mapNotNull { it }
                .distinctUntilChanged()
                .scan(AcfMomentsBO.EMPTY) { acc, values -> accumulate(acc, values, decay) }
                // Drop the seed: it carries no observation and would only make callers special-case it.
                .drop(1)
        }

        /**
         * Folds one ACF curve into the moments, decaying the prior accumulator by [decay] first
         * (`decay = 1f` accumulates over the whole session). Lag 0 is dropped — its correlation is
         * always 1, so it would contribute a constant, variance-free dimension. A dimension mismatch
         * (including the empty accumulator on the first curve) restarts from [acf], so the moments
         * always match the current curve length.
         */
        internal fun accumulate(
            acc: AcfMomentsBO,
            acf: List<Float>,
            decay: Float,
        ): AcfMomentsBO {
            val x = acf.drop(ACF_PCA_FIRST_LAG).map { it.toDouble() }
            val d = decay.toDouble()
            val restart = acc.dimension != x.size
            val priorWeight = if (restart) 0.0 else acc.weight * d
            return AcfMomentsBO(
                observations = if (restart) 1 else acc.observations + 1,
                weight = priorWeight + 1.0,
                latest = x,
                sum = x.mapIndexed { i, v -> (if (restart) 0.0 else acc.sum[i] * d) + v },
                sumOuter =
                    x.indices.map { i ->
                        x.indices.map { j ->
                            (if (restart) 0.0 else acc.sumOuter[i][j] * d) + x[i] * x[j]
                        }
                    },
            )
        }
    }
