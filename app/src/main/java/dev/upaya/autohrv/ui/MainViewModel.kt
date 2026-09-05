package dev.upaya.autohrv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.upaya.autohrv.data.hrv.ConnectionState
import dev.upaya.autohrv.data.hrv.HrvRepository
import dev.upaya.autohrv.data.settings.BreathingSettingsRepository
import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.model.BreathingPatternBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPhaseBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPhaseStartBO
import dev.upaya.autohrv.domain.metrics.MetricsBusiness
import dev.upaya.autohrv.domain.spectral.SpectrogramBusiness
import dev.upaya.autohrv.domain.spectral.model.SpectrogramBandInfoBO
import dev.upaya.autohrv.domain.spectral.model.SpectrogramSliceBO
import dev.upaya.autohrv.ui.acf.shapeAcfHistogram
import dev.upaya.autohrv.ui.commons.Sample
import dev.upaya.autohrv.ui.commons.pruneOlderThan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val acfHistorySeconds: Int = 0,
    val isInResonance: Boolean = false,
    val lagSeconds: Float? = null,
    val spectrogramHistorySeconds: Int = 0,
    val currentPhaseStart: BreathingPhaseStartBO = BreathingPhaseStartBO(BreathingPhaseBO.Inhale, System.currentTimeMillis(), 4000L),
    val currentPattern: BreathingPatternBO = BreathingPatternBO(0f, 8f),
)

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
        private val breathingBusiness: BreathingBusiness,
        private val spectrogramBusiness: SpectrogramBusiness,
        private val metricsBusiness: MetricsBusiness,
        private val breathingSettingsRepository: BreathingSettingsRepository,
    ) : ViewModel() {

        val deviceId: String = HrvRepository.DEVICE_ID
        val acfWindowSeconds: Int = breathingBusiness.acfWindowSeconds
        val spectrogramBands: List<SpectrogramBandInfoBO> = spectrogramBusiness.bands

        /** Seconds of history before the first (fastest) band appears — drives the loading placeholder. */
        val spectrogramWindowSeconds: Int = spectrogramBusiness.firstBandWindowSeconds

        private val _uiState = MutableStateFlow(MainUiState())
        val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

        /** Raw beats from the sensor, each stamped with wall-clock arrival time. */
        val rrSamples: StateFlow<List<Sample>> =
            hrvRepository.rrMsBeatFlow
                .map { rr -> Sample(System.currentTimeMillis(), rr.toFloat()) }
                .scan(emptyList<Sample>()) { acc, s -> (acc + s).pruneOlderThan(DISPLAY_WINDOW_MS, s.tMillis) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        /** Rolling spectrogram slices per band, index-aligned with [spectrogramBands]. */
        val spectrogramBandSlices: StateFlow<List<List<SpectrogramSliceBO>>> = spectrogramBusiness.bandSlices

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
                metricsBusiness.hrvMetrics.collect { metrics ->
                    _uiState.update { it.copy(rmssd = metrics.rmssd) }
                }
            }
            viewModelScope.launch {
                breathingBusiness.autoCorrelation.collect { acf ->
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
                breathingBusiness.acfSums.collect { sums ->
                    _uiState.update { it.copy(acfHistogram = shapeAcfHistogram(sums)) }
                }
            }
            viewModelScope.launch {
                breathingBusiness.acfHistorySeconds.collect { seconds ->
                    _uiState.update { it.copy(acfHistorySeconds = seconds) }
                }
            }
            viewModelScope.launch {
                breathingBusiness.isInResonance.collect { isInResonance ->
                    _uiState.update { it.copy(isInResonance = isInResonance) }
                }
            }
            viewModelScope.launch {
                breathingBusiness.lagSeconds.collect { lag ->
                    _uiState.update { it.copy(lagSeconds = lag) }
                }
            }
            viewModelScope.launch {
                spectrogramBusiness.historySeconds.collect { seconds ->
                    _uiState.update { it.copy(spectrogramHistorySeconds = seconds) }
                }
            }
            viewModelScope.launch {
                breathingBusiness.currentPhaseStart.collect { phaseStart ->
                    _uiState.update { it.copy(currentPhaseStart = phaseStart) }
                }
            }
            viewModelScope.launch {
                breathingBusiness.currentBreathingPattern.collect { pattern ->
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

        fun setTargetInOutBias(bias: Float) = breathingSettingsRepository.setTargetInOutBias(bias)
    }
