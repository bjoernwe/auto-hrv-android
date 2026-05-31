package com.polar.polarsdkecghrdemo.domain.experiment

import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.di.ApplicationScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord
import com.polar.polarsdkecghrdemo.domain.breathing.defaultParams
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
    timeSeriesStatsUseCase: TimeSeriesStatsUseCase,
    polarRepository: PolarRepository,
) {
    private val experimentConfig = ExperimentConfig.DEFAULT

    private val rrsMsHistory: StateFlow<List<Int>> = polarRepository.getRrsMsHistory(experimentConfig.evaluationLengthSeconds)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<TimeSeriesStats?> = timeSeriesStatsUseCase(rrsMsHistory)
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val initialBreathingPattern = experimentConfig.defaultParams()

    private val experiments: StateFlow<BreathingExperiment> =
        breathingExperimentsUseCase(experimentConfig) { stats.value?.periodicity }
            .stateIn(scope, SharingStarted.Eagerly, BreathingExperiment(initialBreathingPattern, initialBreathingPattern))

    val samplingMean: StateFlow<BreathingPattern> = experiments
        .map { it.samplingMean }
        .stateIn(scope, SharingStarted.Eagerly, initialBreathingPattern)

    private val targetBreathingPattern: StateFlow<BreathingPattern> = experiments
        .map { it.candidate }
        .stateIn(scope, SharingStarted.Eagerly, initialBreathingPattern)

    private val pacerOutput = breathingPacerUseCase(scope, targetBreathingPattern)

    val currentBreathingState: StateFlow<BreathingState> = pacerOutput.breathingState
    val currentBreathingPattern: StateFlow<BreathingPattern> = pacerOutput.currentPattern

    val experimentRecords: StateFlow<List<ExperimentRecord>> = currentBreathingPattern
        // keep previous event, not current
        .scan(currentBreathingPattern.value to currentBreathingPattern.value) { (_, current), next -> current to next }
        .map { it.first }
        // the initial value change is not a finished experiment yet
        .drop(1)
        // create record
        .map { finishedPattern -> stats.value?.periodicity?.let { p -> ExperimentRecord(finishedPattern, p) } }
        .filterNotNull()
        // keep history
        .scan(emptyList<ExperimentRecord>()) { records, record -> records + record }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
}
