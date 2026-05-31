package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.di.ApplicationScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord
import com.polar.polarsdkecghrdemo.domain.hr.CalcHrStatsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentCoordinator @Inject internal constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    breathingExperimentsUseCase: BreathingExperimentsUseCase,
    calcHrStatsUseCase: CalcHrStatsUseCase,
    polarRepository: PolarRepository,
) {
    val hrHistory: SharedFlow<List<Int>> = polarRepository.getHrHistory(30)
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val currentBreathingPattern: SharedFlow<BreathingPattern> =
        breathingExperimentsUseCase(ExperimentConfig.DEFAULT)
            .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val periodicity: StateFlow<Float?> = calcHrStatsUseCase.periodicity(hrHistory)
        .stateIn(scope, SharingStarted.Eagerly, null)

    val experimentRecords: StateFlow<List<ExperimentRecord>> = currentBreathingPattern
        .scan((null as BreathingPattern?) to emptyList<ExperimentRecord>()) { (prev, records), current ->
            val updated = periodicity.value?.let { p ->
                if (prev != null) records + ExperimentRecord(prev, p) else records
            } ?: records
            current to updated
        }
        .map { (_, records) -> records }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
}
