package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.di.ApplicationScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingParams
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord
import com.polar.polarsdkecghrdemo.domain.breathing.GenerateBreathingParamsUseCase
import com.polar.polarsdkecghrdemo.domain.hr.HeartRateStatsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentCoordinator @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    generateBreathingParamsUseCase: GenerateBreathingParamsUseCase,
    heartRateStatsUseCase: HeartRateStatsUseCase,
    repository: PolarRepository,
) {
    val hrHistory: SharedFlow<List<Int>> = repository.getHrHistory(30)
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    private val paramsFlow: SharedFlow<BreathingParams> =
        generateBreathingParamsUseCase(ExperimentConfig.DEFAULT)
            .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val currentParams: StateFlow<BreathingParams> = paramsFlow.stateIn(
        scope,
        SharingStarted.Eagerly,
        BreathingParams(ExperimentConfig.DEFAULT.outToInRatioMean, ExperimentConfig.DEFAULT.cycleLengthMean),
    )

    val periodicity: Flow<Float?> = heartRateStatsUseCase.periodicity(hrHistory)

    private val _history = MutableStateFlow<List<ExperimentRecord>>(emptyList())
    val history: StateFlow<List<ExperimentRecord>> = _history.asStateFlow()

    init {
        scope.launch {
            val latestPeriodicity = MutableStateFlow<Float?>(null)
            launch { periodicity.filterNotNull().collect { latestPeriodicity.value = it } }

            var prev: BreathingParams? = null
            paramsFlow.collect { current ->
                latestPeriodicity.value?.let { p ->
                    prev?.let { _history.update { h -> h + ExperimentRecord(it, p) } }
                }
                prev = current
            }
        }
    }
}
