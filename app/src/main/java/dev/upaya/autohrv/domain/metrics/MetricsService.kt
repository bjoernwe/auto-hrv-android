package dev.upaya.autohrv.domain.metrics

import dev.upaya.autohrv.data.hrv.HrvRepository
import dev.upaya.autohrv.di.ApplicationScope
import dev.upaya.autohrv.domain.metrics.model.HrvMetricsBO
import dev.upaya.autohrv.domain.metrics.usecase.ComputeHrvMetricsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsService
    @Inject
    internal constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
        computeHrvMetricsUseCase: ComputeHrvMetricsUseCase,
        hrvRepository: HrvRepository,
    ) {
        private val config = MetricsConfig.DEFAULT

        val hrvMetrics: StateFlow<HrvMetricsBO> =
            computeHrvMetricsUseCase(hrvRepository.getRrsMsBeatHistory(config.beatWindowSeconds))
                .stateIn(scope, SharingStarted.Eagerly, HrvMetricsBO(sdrr = null, rmssd = null))
    }
