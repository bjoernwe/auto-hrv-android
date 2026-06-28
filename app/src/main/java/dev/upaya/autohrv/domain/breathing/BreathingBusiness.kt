package dev.upaya.autohrv.domain.breathing

import dev.upaya.autohrv.data.repository.HrvRepository
import dev.upaya.autohrv.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    val currentPhaseStart: StateFlow<BreathingPhaseStart> = pacerOutput.currentPhaseStart
    val currentBreathingPattern: StateFlow<BreathingPattern> = pacerOutput.currentPattern

    val isInResonance: StateFlow<Boolean> = combine(stats, currentBreathingPattern) { stats, breathingPattern ->
        val rrsStats = stats?.resampledRrsStats ?: return@combine false
        val peak = rrsStats.autoCorrelationPeak ?: return@combine false
        val peakValue = rrsStats.autoCorrelation?.getOrNull(peak.toInt()) ?: return@combine false
        abs(peak - breathingPattern.cycleLengthSeconds) <= breathingConfig.resonancePeakToleranceSeconds && peakValue > breathingConfig.resonanceMinPeakValue
    }.stateIn(scope, SharingStarted.Eagerly, false)

    // Breath signal sampled at 1 Hz to match the RR history grid.
    private val breathHistory: StateFlow<List<Float>> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(currentPhaseStart.value.valueAt(now))
            delay(1000L)
        }
    }
        .scan(emptyList<Float>()) { w, v -> (w + v).takeLast(breathingConfig.evaluationLengthSeconds) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Seconds by which the RR response lags behind the breath signal (positive = heart follows breath).
    val lagSeconds: StateFlow<Float?> = combine(breathHistory, rrsMsHistory) { breath, rr ->
        computeLag(breath, rr, breathingConfig.maxCycleLengthRange.endInclusive)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private fun computeLag(breath: List<Float>, rr: List<Int>, maxCycleLengthSeconds: Float): Float? {
        val n = minOf(breath.size, rr.size)
        if (n < 4) return null
        val b = breath.takeLast(n)
        val r = rr.takeLast(n)
        val bMean = b.average().toFloat()
        val rMean = r.average().toFloat()
        val bNorm = b.map { it - bMean }
        val rNorm = r.map { it.toFloat() - rMean }
        // RR is anti-phase to breath (HR rises on inhale → RR drops), so correlate breath vs –RR.
        // Peak at lag τ means the heart responds τ seconds after the breath signal.
        val maxLag = maxCycleLengthSeconds.toInt().coerceAtMost(n / 2)
        return (0..maxLag).maxByOrNull { lag ->
            (0 until n - lag).sumOf { t -> (bNorm[t] * (-rNorm[t + lag])).toDouble() }
        }?.toFloat()
    }
}
