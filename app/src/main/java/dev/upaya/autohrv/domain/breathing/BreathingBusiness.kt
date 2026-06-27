package dev.upaya.autohrv.domain.breathing

import dev.upaya.autohrv.data.repository.HrvRepository
import dev.upaya.autohrv.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class BreathingBusiness @Inject internal constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    breathingPacerUseCase: BreathingPacerUseCase,
    timeSeriesStatsUseCase: TimeSeriesStatsUseCase,
    hrvRepository: HrvRepository,
) {
    private val breathingConfig = BreathingConfig.DEFAULT

    private val _targetOutToInRatio = MutableStateFlow(breathingConfig.outToInRatio)
    val targetOutToInRatio: StateFlow<Float> = _targetOutToInRatio

    fun setTargetOutToInRatio(ratio: Float) {
        _targetOutToInRatio.value = ratio
    }

    val cycleLengthAllowedRange: ClosedFloatingPointRange<Float> = breathingConfig.maxCycleLengthRange

    private val _targetCycleLengthRange = MutableStateFlow(breathingConfig.initialCycleLengthRange)
    val targetCycleLengthRange: StateFlow<ClosedFloatingPointRange<Float>> = _targetCycleLengthRange

    fun setTargetCycleLengthRange(range: ClosedFloatingPointRange<Float>) {
        _targetCycleLengthRange.value = range
    }

    private val rrsMsHistory: StateFlow<List<Int>> = hrvRepository.getRrsMsHistory(breathingConfig.evaluationLengthSeconds)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val rrsMsBeatHistory: StateFlow<List<Int>> = hrvRepository.getRrsMsBeatHistory(breathingConfig.evaluationLengthSeconds)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<TimeSeriesStats?> = _targetCycleLengthRange.flatMapLatest { range ->
        timeSeriesStatsUseCase(rrsMsHistory, rrsMsBeatHistory, range)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val initialBreathingPattern = breathingConfig.defaultPattern()

    private val smoothedTargetCycleLength: Flow<Float> = combine(stats, _targetCycleLengthRange) {
        s, range -> (s?.resampledRrsStats?.autoCorrelationPeak ?: breathingConfig.initialCycleLength).coerceIn(range)
    }
        .scan(emptyList<Float>()) { window, cl -> (window + cl).takeLast(breathingConfig.evaluationLengthSeconds) }
        .filter { it.isNotEmpty() }
        .map { window -> window.reduce { a, b -> a + b } / window.size.toFloat() }

    private val targetBreathingPattern: StateFlow<BreathingPattern> =
        combine(smoothedTargetCycleLength, targetOutToInRatio) { cl, ratio ->
            BreathingPattern(ratio, cl)
        }
        .stateIn(scope, SharingStarted.Eagerly, initialBreathingPattern)

    private val pacerOutput = breathingPacerUseCase(scope, targetBreathingPattern)

    val currentBreathingState: StateFlow<BreathingState> = pacerOutput.breathingState
    val currentBreathingPattern: StateFlow<BreathingPattern> = pacerOutput.currentPattern

    val isInResonance: StateFlow<Boolean> = combine(stats, currentBreathingPattern) { stats, breathingPattern ->
        val rrsStats = stats?.resampledRrsStats ?: return@combine false
        val peak = rrsStats.autoCorrelationPeak ?: return@combine false
        val peakValue = rrsStats.autoCorrelation?.getOrNull(peak.toInt()) ?: return@combine false
        abs(peak - breathingPattern.cycleLengthSeconds) <= breathingConfig.resonancePeakToleranceSeconds && peakValue > breathingConfig.resonanceMinPeakValue
    }.stateIn(scope, SharingStarted.Eagerly, false)
}
