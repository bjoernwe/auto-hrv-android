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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
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

    private val _targetOutToInRatio = MutableStateFlow(experimentConfig.outToInRatioMean)
    val targetOutToInRatio: StateFlow<Float> = _targetOutToInRatio

    fun setTargetOutToInRatio(ratio: Float) {
        _targetOutToInRatio.value = ratio
    }

    private val _cycleLengthRange = MutableStateFlow(4f..20f)
    val cycleLengthRange: StateFlow<ClosedFloatingPointRange<Float>> = _cycleLengthRange

    fun setTargetCycleLengthRange(range: ClosedFloatingPointRange<Float>) {
        _cycleLengthRange.value = range
    }

    private val rrsMsHistory: StateFlow<List<Int>> = polarRepository.getRrsMsHistory(experimentConfig.evaluationLengthSeconds)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val rrsMsBeatHistory: StateFlow<List<Int>> = polarRepository.getRrsMsBeatHistory(experimentConfig.evaluationLengthSeconds)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<TimeSeriesStats?> = timeSeriesStatsUseCase(rrsMsHistory, rrsMsBeatHistory)
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val objective: () -> Float = {
        val peakPower = stats.value?.resampledRrsStats?.peakPower ?: 0f
        val periodicity = stats.value?.resampledRrsStats?.periodicity ?: 0f
        val smoothness = stats.value?.resampledRrsStats?.smoothness ?: 0f
        val sdrr = stats.value?.beatRrsStats?.sdrr ?: 0f
        0 - peakPower.div(1_000_000) - periodicity - smoothness.div(3) - sdrr.div(200)
    }

    private val initialBreathingPattern = experimentConfig.defaultParams()

    private val experiments: StateFlow<BreathingExperiment> =
        breathingExperimentsUseCase(experimentConfig, objective)
            .stateIn(scope, SharingStarted.Eagerly, BreathingExperiment(initialBreathingPattern, initialBreathingPattern))

    val samplingMean: StateFlow<BreathingPattern> = experiments
        .map { it.samplingMean }
        .stateIn(scope, SharingStarted.Eagerly, initialBreathingPattern)

    private val smoothedCycleLength: Flow<Float> = stats
        .map { s -> s?.resampledRrsStats?.autoCorrelationPeak ?: experimentConfig.cycleLengthMean }
        .map { s -> s.coerceIn(_cycleLengthRange.value) }
        .scan(emptyList<Float>()) { window, cl -> (window + cl).takeLast(experimentConfig.experimentLengthSeconds) }
        .filter { it.isNotEmpty() }
        .map { window -> window.reduce { a, b -> a + b } / window.size.toFloat() }

    private val targetBreathingPattern: StateFlow<BreathingPattern> =
        combine(smoothedCycleLength, targetOutToInRatio) { cl, ratio ->
            BreathingPattern(ratio, cl)
        }
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
        .map { finishedPattern -> stats.value?.resampledRrsStats?.periodicity?.let { p -> ExperimentRecord(finishedPattern, p) } }
        .filterNotNull()
        // keep history
        .scan(emptyList<ExperimentRecord>()) { records, record -> records + record }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
}
