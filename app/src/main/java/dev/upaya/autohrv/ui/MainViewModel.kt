package dev.upaya.autohrv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.upaya.autohrv.data.hrv.ConnectionState
import dev.upaya.autohrv.data.hrv.HrvRepository
import dev.upaya.autohrv.data.settings.BreathingSettingsRepository
import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.BreathingService
import dev.upaya.autohrv.domain.breathing.model.BreathingPatternBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPhaseBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPhaseStartBO
import dev.upaya.autohrv.domain.metrics.MetricsService
import dev.upaya.autohrv.domain.spectral.SpectrogramService
import dev.upaya.autohrv.domain.spectral.model.SpectrogramBandInfoBO
import dev.upaya.autohrv.domain.spectral.model.SpectrogramSliceBO
import dev.upaya.autohrv.ui.acf.AcfBarStyle
import dev.upaya.autohrv.ui.acf.shapeAcfHistogram
import dev.upaya.autohrv.ui.acf.shapeAcfLoadings
import dev.upaya.autohrv.ui.commons.Sample
import dev.upaya.autohrv.ui.commons.pruneOlderThan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class MainUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val hr: Int? = null,
    val currentRr: Int? = null,
    val batteryLevel: Int? = null,
    val rmssd: Float? = null,
    val swing: Int? = null,
    val autoCorrelation: List<Float>? = null,
    val autoCorrelationPeak: Float? = null,
    val acfHistogram: List<Float> = emptyList(),
    /** Signed bar values for [selectedPrincipalComponent]'s loadings, aligned with the ACF's lags. */
    val acfLoadingBars: List<Float> = emptyList(),
    /** Per-component projection of the current curve, in units of that component's own std dev. */
    val acfComponentScores: List<Float> = emptyList(),
    val acfComponentVarianceRatios: List<Float> = emptyList(),
    val selectedPrincipalComponent: Int? = null,
    val acfHistorySeconds: Int = 0,
    val isInResonance: Boolean = false,
    val lagSeconds: Float? = null,
    val spectrogramHistorySeconds: Int = 0,
    val currentPhaseStart: BreathingPhaseStartBO = BreathingPhaseStartBO(BreathingPhaseBO.Inhale, System.currentTimeMillis(), 4000L),
    val currentPattern: BreathingPatternBO = BreathingPatternBO(0f, 8f),
) {
    /** Whether a principal component is available to select and score. */
    val acfPcaReady: Boolean get() = acfComponentScores.isNotEmpty()

    /** The bars behind the ACF curve: a selected component's loadings, else the session histogram. */
    val acfChartBars: List<Float> get() = if (selectedPrincipalComponent != null) acfLoadingBars else acfHistogram

    /** How [acfChartBars] is drawn — loadings are signed and grow out of the zero line. */
    val acfChartBarStyle: AcfBarStyle
        get() = if (selectedPrincipalComponent != null) AcfBarStyle.FromZeroLine else AcfBarStyle.FromBottom
}

// The direct ACF returns lags 0..acfMaxLagSeconds, so the chart shows the full searchable range.
private val AUTO_CORRELATION_SIZE = BreathingConfig.DEFAULT.acfMaxLagSeconds + 1

private const val BREATH_SAMPLE_RATE_HZ = 20

// Chart display window, independent of any domain window (outlier filtering, HRV metrics, ACF).
private const val DISPLAY_WINDOW_SECONDS = 20
private val DISPLAY_WINDOW_MS = DISPLAY_WINDOW_SECONDS * 1000L

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val hrvRepository: HrvRepository,
        private val breathingService: BreathingService,
        private val spectrogramService: SpectrogramService,
        private val metricsService: MetricsService,
        private val breathingSettingsRepository: BreathingSettingsRepository,
    ) : ViewModel() {

        val deviceId: String = HrvRepository.DEVICE_ID
        val acfWindowSeconds: Int = breathingService.acfWindowSeconds

        /** How many principal-component toggles the ACF header offers. */
        val acfPcaComponentCount: Int = breathingService.acfPcaComponentCount
        val spectrogramBands: List<SpectrogramBandInfoBO> = spectrogramService.bands

        /** Seconds of history before the first (fastest) band appears — drives the loading placeholder. */
        val spectrogramWindowSeconds: Int = spectrogramService.firstBandWindowSeconds

        private val _uiState = MutableStateFlow(MainUiState())
        val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

        /**
         * Which principal component the ACF chart draws, or `null` for the accumulated histogram.
         * Pure view state — no domain derivation reads it — so it lives here rather than in a
         * settings repository.
         */
        private val selectedPrincipalComponentSelection = MutableStateFlow<Int?>(null)

        /** Raw beats from the sensor, each stamped with wall-clock arrival time. */
        val rrSamples: StateFlow<List<Sample>> =
            hrvRepository.rrMsBeatFlow
                .map { rr -> Sample(System.currentTimeMillis(), rr.toFloat()) }
                .scan(emptyList<Sample>()) { acc, s -> (acc + s).pruneOlderThan(DISPLAY_WINDOW_MS, s.tMillis) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        /** Rolling spectrogram slices per band, index-aligned with [spectrogramBands]. */
        val spectrogramBandSlices: StateFlow<List<List<SpectrogramSliceBO>>> = spectrogramService.bandSlices

        init {
            viewModelScope.launch {
                hrvRepository.connectionState.collect { state ->
                    _uiState.update { it.copy(connectionState = state) }
                }
            }
            viewModelScope.launch {
                hrvRepository.batteryLevel.collect { level ->
                    _uiState.update { it.copy(batteryLevel = level) }
                }
            }
            viewModelScope.launch {
                hrvRepository.hrResampled1Hz.collect { hr ->
                    _uiState.update { it.copy(hr = hr) }
                }
            }
            viewModelScope.launch {
                rrSamples.collect { samples ->
                    val values = samples.map { it.value.toInt() }
                    val swing = if (values.size >= 2) values.max() - values.min() else null
                    _uiState.update { it.copy(currentRr = values.lastOrNull(), swing = swing) }
                }
            }
            viewModelScope.launch {
                metricsService.hrvMetrics.collect { metrics ->
                    _uiState.update { it.copy(rmssd = metrics.rmssd) }
                }
            }
            viewModelScope.launch {
                breathingService.autoCorrelation.collect { acf ->
                    _uiState.update { uiState ->
                        uiState.copy(
                            autoCorrelation =
                                acf
                                    ?.values
                                    ?.takeIf { it.size >= AUTO_CORRELATION_SIZE }
                                    ?.take(AUTO_CORRELATION_SIZE),
                            autoCorrelationPeak = acf?.peakLagSeconds,
                        )
                    }
                }
            }
            viewModelScope.launch {
                breathingService.acfSums.collect { sums ->
                    _uiState.update { it.copy(acfHistogram = shapeAcfHistogram(sums)) }
                }
            }
            viewModelScope.launch {
                combine(breathingService.acfPca, selectedPrincipalComponentSelection) { pca, selection ->
                    val components = pca?.components.orEmpty()
                    val selected = selection?.takeIf { it in components.indices }
                    val loadings = selected?.let { components[it].loadings }
                    _uiState.update { state ->
                        state.copy(
                            acfLoadingBars =
                                if (pca != null && loadings != null) {
                                    shapeAcfLoadings(loadings, pca.firstLag, AUTO_CORRELATION_SIZE)
                                } else {
                                    emptyList()
                                },
                            // Standardized so PC1 and PC3 read on the same scale despite their very
                            // different magnitudes; a component with no spread has no meaningful score.
                            acfComponentScores =
                                components.map { component ->
                                    if (component.standardDeviation > 0f) component.score / component.standardDeviation else 0f
                                },
                            acfComponentVarianceRatios = components.map { it.explainedVarianceRatio },
                            selectedPrincipalComponent = selected,
                        )
                    }
                }.collect()
            }
            viewModelScope.launch {
                breathingService.acfHistorySeconds.collect { seconds ->
                    _uiState.update { it.copy(acfHistorySeconds = seconds) }
                }
            }
            viewModelScope.launch {
                breathingService.isInResonance.collect { isInResonance ->
                    _uiState.update { it.copy(isInResonance = isInResonance) }
                }
            }
            viewModelScope.launch {
                breathingService.lagSeconds.collect { lag ->
                    _uiState.update { it.copy(lagSeconds = lag) }
                }
            }
            viewModelScope.launch {
                spectrogramService.historySeconds.collect { seconds ->
                    _uiState.update { it.copy(spectrogramHistorySeconds = seconds) }
                }
            }
            viewModelScope.launch {
                breathingService.currentPhaseStart.collect { phaseStart ->
                    _uiState.update { it.copy(currentPhaseStart = phaseStart) }
                }
            }
            viewModelScope.launch {
                breathingService.currentBreathingPattern.collect { pattern ->
                    _uiState.update { it.copy(currentPattern = pattern) }
                }
            }
        }

        /** Pacer function sampled at 20 Hz, each point stamped with real wall-clock time. */
        val breathSamples: StateFlow<List<Sample>> =
            flow {
                while (true) {
                    val t = System.currentTimeMillis()
                    emit(Sample(t, _uiState.value.currentPhaseStart.valueAt(t)))
                    delay((1000L / BREATH_SAMPLE_RATE_HZ).milliseconds)
                }
            }.scan(emptyList<Sample>()) { acc, s -> (acc + s).pruneOlderThan(DISPLAY_WINDOW_MS, s.tMillis) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val displayWindowMs: Long = DISPLAY_WINDOW_MS

        val targetCycleLengthRange: StateFlow<IntRange> = breathingSettingsRepository.targetCycleLengthRange
        val cycleLengthAllowedRange: IntRange = breathingSettingsRepository.cycleLengthAllowedRange
        val targetInOutBias: StateFlow<Float> = breathingSettingsRepository.targetInOutBias

        fun connect() = hrvRepository.connect()

        fun disconnect() = hrvRepository.disconnect()

        fun setTargetCycleLengthRange(range: IntRange) = breathingSettingsRepository.setTargetCycleLengthRange(range)

        fun selectPrincipalComponent(index: Int?) {
            selectedPrincipalComponentSelection.value = index
        }

        fun setTargetInOutBias(bias: Float) = breathingSettingsRepository.setTargetInOutBias(bias)
    }
