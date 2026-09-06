package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.model.ACF_PCA_FIRST_LAG
import dev.upaya.autohrv.domain.breathing.model.AcfMomentsBO
import dev.upaya.autohrv.domain.breathing.model.AcfPcaBO
import dev.upaya.autohrv.domain.breathing.model.AcfPrincipalComponentBO
import dev.upaya.autohrv.domain.signal.PrincipalComponent
import dev.upaya.autohrv.domain.signal.principalComponents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Principal components of the ACF curves collected over the session, plus the projection of the
 * current curve onto each of them.
 *
 * Each curve is one point in `R^maxLag` (lags `1..maxLag`), so the components describe which lags
 * rise and fall together — a component loading broadly on the slow lags, say, is a different fact
 * about the signal than the single strongest lag the peak search returns.
 *
 * Reads the current curve from [AcfMomentsBO.latest] rather than from the ACF flow directly, so one
 * new curve means exactly one decomposition.
 *
 * Emits `null` until [BreathingConfig.acfPcaMinObservations] curves have been seen: below that the
 * covariance is rank-deficient and its trailing components are noise.
 */
internal class ComputeAcfPcaUseCase
    @Inject
    constructor() {

        operator fun invoke(
            moments: Flow<AcfMomentsBO>,
            config: BreathingConfig,
        ): Flow<AcfPcaBO?> =
            moments
                .map { computeAcfPca(it, config) }
                // An eigenvector's sign is arbitrary, so a fresh decomposition can come back inverted
                // from one second to the next. Carry the previous result and align each component with
                // its predecessor so the displayed loadings hold still.
                .scan<AcfPcaBO?, AcfPcaBO?>(null) { previous, current -> alignSigns(previous, current) }

        private fun computeAcfPca(
            moments: AcfMomentsBO,
            config: BreathingConfig,
        ): AcfPcaBO? {
            if (moments.observations < config.acfPcaMinObservations || moments.weight <= 0.0) return null

            val mean = moments.sum.map { it / moments.weight }
            val covariance =
                mean.indices.map { i ->
                    mean.indices.map { j -> moments.sumOuter[i][j] / moments.weight - mean[i] * mean[j] }
                }

            val basis = principalComponents(covariance, config.acfPcaComponentCount)
            if (basis.components.isEmpty()) return null

            val centered = moments.latest.mapIndexed { i, v -> v - mean[i] }
            return AcfPcaBO(
                firstLag = ACF_PCA_FIRST_LAG,
                components =
                    basis.components.map { component ->
                        AcfPrincipalComponentBO(
                            loadings = component.loadings,
                            explainedVarianceRatio = component.variance / basis.totalVariance,
                            standardDeviation = sqrt(component.variance),
                            score = project(centered, component),
                        )
                    },
            )
        }

        private fun project(
            centered: List<Double>,
            component: PrincipalComponent,
        ): Float = centered.indices.sumOf { i -> centered[i] * component.loadings[i] }.toFloat()

        /**
         * Flips any component of [current] that points away from the same-ranked component of
         * [previous], negating its loadings and score together so the projection stays consistent.
         *
         * Only the sign is stabilized: when two eigenvalues cross, the components genuinely swap
         * rank and the view follows that swap — expected for an exploratory display.
         */
        private fun alignSigns(
            previous: AcfPcaBO?,
            current: AcfPcaBO?,
        ): AcfPcaBO? {
            if (previous == null || current == null) return current
            return current.copy(
                components =
                    current.components.mapIndexed { i, component ->
                        val prior = previous.components.getOrNull(i) ?: return@mapIndexed component
                        if (prior.loadings.size != component.loadings.size) return@mapIndexed component
                        val dot = component.loadings.indices.sumOf { k -> (component.loadings[k] * prior.loadings[k]).toDouble() }
                        if (dot >= 0.0) {
                            component
                        } else {
                            component.copy(loadings = component.loadings.map { -it }, score = -component.score)
                        }
                    },
            )
        }
    }
