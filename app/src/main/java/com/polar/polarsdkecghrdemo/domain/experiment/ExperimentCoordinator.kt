package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.di.ApplicationScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingParams
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord
import com.polar.polarsdkecghrdemo.domain.breathing.GenerateBreathingExperimentsUseCase
import com.polar.polarsdkecghrdemo.domain.hr.CalcHrStatsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentCoordinator @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    generateBreathingExperimentsUseCase: GenerateBreathingExperimentsUseCase,
    calcHrStatsUseCase: CalcHrStatsUseCase,
    polarRepository: PolarRepository,
) {
    val hrHistory: SharedFlow<List<Int>> = polarRepository.getHrHistory(30)
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val currentBreathingPattern: SharedFlow<BreathingParams> =
        generateBreathingExperimentsUseCase(ExperimentConfig.DEFAULT)
            .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val periodicity: Flow<Float?> = calcHrStatsUseCase.periodicity(hrHistory)

    private val _experimentRecords = MutableStateFlow<List<ExperimentRecord>>(emptyList())
    val experimentRecords: StateFlow<List<ExperimentRecord>> = _experimentRecords.asStateFlow()

    init {
        scope.launch {
            val latestPeriodicity = MutableStateFlow<Float?>(null)
            launch { periodicity.filterNotNull().collect { latestPeriodicity.value = it } }

            var prev: BreathingParams? = null
            currentBreathingPattern.collect { current ->
                latestPeriodicity.value?.let { p ->
                    prev?.let { _experimentRecords.update { h -> h + ExperimentRecord(it, p) } }
                }
                prev = current
            }
        }
    }
}
