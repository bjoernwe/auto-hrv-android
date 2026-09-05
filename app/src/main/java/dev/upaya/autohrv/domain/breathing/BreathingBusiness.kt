package dev.upaya.autohrv.domain.breathing

import dev.upaya.autohrv.data.hrv.HrvRepository
import dev.upaya.autohrv.data.settings.BreathingSettingsRepository
import dev.upaya.autohrv.di.ApplicationScope
import dev.upaya.autohrv.domain.breathing.model.AutoCorrelationBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPatternBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPhaseStartBO
import dev.upaya.autohrv.domain.breathing.usecase.AccumulateAcfUseCase
import dev.upaya.autohrv.domain.breathing.usecase.ComputeAutoCorrelationUseCase
import dev.upaya.autohrv.domain.breathing.usecase.ComputeBreathRrLagUseCase
import dev.upaya.autohrv.domain.breathing.usecase.DetectResonanceUseCase
import dev.upaya.autohrv.domain.breathing.usecase.RunBreathingPacerUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class BreathingBusiness
    @Inject
    internal constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
        computeAutoCorrelationUseCase: ComputeAutoCorrelationUseCase,
        accumulateAcfUseCase: AccumulateAcfUseCase,
        runBreathingPacerUseCase: RunBreathingPacerUseCase,
        detectResonanceUseCase: DetectResonanceUseCase,
        computeBreathRrLagUseCase: ComputeBreathRrLagUseCase,
        hrvRepository: HrvRepository,
        settings: BreathingSettingsRepository,
    ) {
        private val breathingConfig = BreathingConfig.DEFAULT

        private val rrsMsHistory: StateFlow<List<Int>> =
            hrvRepository
                .getRrsMs1HzHistory(breathingConfig.acfWindowSeconds)
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

        /** Seconds of 1 Hz RR history the ACF needs before it can compute a first estimate. */
        val acfWindowSeconds: Int = breathingConfig.acfWindowSeconds

        // rrsMsHistory holds exactly one sample per second (capped at acfWindowSeconds), so its
        // size doubles as "seconds of history collected so far" — the raw fact the ACF card's
        // loading progress is derived from. Left as a count rather than a 0..1 fraction since the
        // normalization is a display concern, not a domain one.
        val acfHistorySeconds: StateFlow<Int> =
            rrsMsHistory
                .map { it.size }
                .stateIn(scope, SharingStarted.Eagerly, 0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val autoCorrelation: StateFlow<AutoCorrelationBO?> =
            settings.targetCycleLengthRange
                .flatMapLatest { range -> computeAutoCorrelationUseCase(rrsMsHistory, range, breathingConfig) }
                .stateIn(scope, SharingStarted.Eagerly, null)

        // Session-accumulated per-lag ACF sums, raw — shaping into [0, 1] chart heights is a UI concern.
        val acfSums: StateFlow<List<Float>> =
            accumulateAcfUseCase(autoCorrelation.map { it?.values }, breathingConfig)
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

        private val initialBreathingPattern = breathingConfig.defaultPattern()

        private val smoothedTargetCycleLength: Flow<Float> =
            combine(autoCorrelation, settings.targetCycleLengthRange) { acf, range ->
                (acf?.peakLagSeconds ?: breathingConfig.initialCycleLength)
                    .coerceIn(range.first.toFloat(), range.last.toFloat())
            }.scan(emptyList<Float>()) { window, cl -> (window + cl).takeLast(breathingConfig.targetCycleLengthSmoothingWindow) }
                .filter { it.isNotEmpty() }
                .map { window -> window.reduce { a, b -> a + b } / window.size.toFloat() }

        private val targetBreathingPattern: StateFlow<BreathingPatternBO> =
            combine(smoothedTargetCycleLength, settings.targetInOutBias) { cl, bias ->
                BreathingPatternBO(bias, cl)
            }.stateIn(scope, SharingStarted.Eagerly, initialBreathingPattern)

        private val pacerOutput = runBreathingPacerUseCase(targetBreathingPattern)

        val currentPhaseStart: StateFlow<BreathingPhaseStartBO> = pacerOutput.currentPhaseStart
        val currentBreathingPattern: StateFlow<BreathingPatternBO> = pacerOutput.currentPattern

        val isInResonance: StateFlow<Boolean> =
            detectResonanceUseCase(autoCorrelation, currentBreathingPattern, breathingConfig)
                .stateIn(scope, SharingStarted.Eagerly, false)

        // Breath signal sampled at 1 Hz to match the RR history grid.
        private val breathHistory: StateFlow<List<Float>> =
            flow {
                while (true) {
                    val now = System.currentTimeMillis()
                    emit(currentPhaseStart.value.valueAt(now))
                    delay(1000L.milliseconds)
                }
            }.scan(emptyList<Float>()) { w, v -> (w + v).takeLast(breathingConfig.acfWindowSeconds) }
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

        // Seconds by which the RR response lags behind the breath signal (positive = heart follows breath).
        val lagSeconds: StateFlow<Float?> =
            computeBreathRrLagUseCase(breathHistory, rrsMsHistory, breathingConfig)
                .stateIn(scope, SharingStarted.Eagerly, null)
    }
