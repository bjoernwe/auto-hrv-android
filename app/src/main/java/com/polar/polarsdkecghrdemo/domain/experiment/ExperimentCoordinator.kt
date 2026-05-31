package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.di.ApplicationScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord
import com.polar.polarsdkecghrdemo.domain.hr.CalcHrStatsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentCoordinator @Inject internal constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    breathingExperimentsUseCase: BreathingExperimentsUseCase,
    breathingPacerUseCase: BreathingPacerUseCase,
    calcHrStatsUseCase: CalcHrStatsUseCase,
    polarRepository: PolarRepository,
) {
    private val targetBreathingPattern: StateFlow<BreathingPattern> =
        breathingExperimentsUseCase(ExperimentConfig.DEFAULT)
            .stateIn(scope, SharingStarted.Eagerly, ExperimentConfig.DEFAULT.defaultParams())

    val currentBreathingState: StateFlow<BreathingState> = breathingPacerUseCase(scope, targetBreathingPattern)

    val hrHistory: StateFlow<List<Int>> = polarRepository.getHrHistory(30)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val periodicity: StateFlow<Float?> = calcHrStatsUseCase.periodicity(hrHistory)
        .stateIn(scope, SharingStarted.Eagerly, null)

    val experimentRecords: StateFlow<List<ExperimentRecord>> = targetBreathingPattern
        // keep previous event, not current
        .scan(targetBreathingPattern.value to targetBreathingPattern.value) { (_, current), next -> current to next }
        .map { it.first }
        // the initial value change is not a finished experiment yet
        .drop(1)
        // create record
        .map { finishedPattern -> periodicity.value?.let { p -> ExperimentRecord(finishedPattern, p) } }
        .filterNotNull()
        // keep history
        .scan(emptyList<ExperimentRecord>()) { records, record -> records + record }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
}
