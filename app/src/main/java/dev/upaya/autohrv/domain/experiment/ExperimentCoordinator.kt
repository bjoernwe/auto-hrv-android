package dev.upaya.autohrv.domain.experiment

import dev.upaya.autohrv.data.repository.HrvRepository
import dev.upaya.autohrv.di.ApplicationScope
import dev.upaya.autohrv.domain.breathing.BreathingPacerUseCase
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentCoordinator @Inject internal constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    breathingPacerUseCase: BreathingPacerUseCase,
    timeSeriesStatsUseCase: TimeSeriesStatsUseCase,
    hrvRepository: HrvRepository,
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

    private val rrsMsHistory: StateFlow<List<Int>> = hrvRepository.getRrsMsHistory(experimentConfig.evaluationLengthSeconds)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val rrsMsBeatHistory: StateFlow<List<Int>> = hrvRepository.getRrsMsBeatHistory(experimentConfig.evaluationLengthSeconds)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<TimeSeriesStats?> = timeSeriesStatsUseCase(rrsMsHistory, rrsMsBeatHistory)
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val initialBreathingPattern = experimentConfig.defaultParams()

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
}
