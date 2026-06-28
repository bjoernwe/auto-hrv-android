package dev.upaya.autohrv.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.upaya.autohrv.data.model.ConnectionState
import dev.upaya.autohrv.data.repository.HrvRepository
import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.domain.breathing.BreathingPhaseStart
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class HrUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val hr: Int? = null,
    val currentRr: Int? = null,
    val batteryLevel: Int? = null,
    val rmssd: Float? = null,
    val swing: Int? = null,
    val autoCorrelation: List<Float>? = null,
    val autoCorrelationPeak: Float? = null,
    val isInResonance: Boolean = false,
    val lagSeconds: Float? = null,
    val currentPhaseStart: BreathingPhaseStart = BreathingPhaseStart(BreathingPhase.Inhale, System.currentTimeMillis(), 4000L),
    val currentPattern: BreathingPattern = BreathingPattern(1f, 8f),
)

const val AUTO_CORRELATION_SIZE = 20

private const val BREATH_SAMPLE_RATE_HZ = 20
private const val DISPLAY_WINDOW_MS = 22_000L

@HiltViewModel
class HrvViewModel @Inject constructor(
    private val hrvRepository: HrvRepository,
    private val breathingBusiness: BreathingBusiness,
) : ViewModel() {

    val deviceId: String = HrvRepository.DEVICE_ID

    private val _uiState = MutableStateFlow(HrUiState())
    val uiState: StateFlow<HrUiState> = _uiState.asStateFlow()

    /** Raw beats from the sensor, each stamped with wall-clock arrival time. */
    val rrSamples: StateFlow<List<Sample>> = hrvRepository.rrMsBeatFlow
        .map { rr -> Sample(System.currentTimeMillis(), rr.toFloat()) }
        .scan(emptyList<Sample>()) { acc, s -> (acc + s).pruneOlderThan(DISPLAY_WINDOW_MS, s.tMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            breathingBusiness.stats.collect { stats ->
                val peak = stats?.resampledRrsStats?.autoCorrelationPeak
                _uiState.update { uiState -> uiState.copy(
                    rmssd = stats?.beatRrsStats?.rmssd,
                    autoCorrelation = stats?.resampledRrsStats?.autoCorrelation?.takeIf { ac -> ac.size >= AUTO_CORRELATION_SIZE }?.take(AUTO_CORRELATION_SIZE),
                    autoCorrelationPeak = peak,
                ) }
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
    val breathSamples: StateFlow<List<Sample>> = flow {
        while (true) {
            val t = System.currentTimeMillis()
            emit(Sample(t, _uiState.value.currentPhaseStart.valueAt(t)))
            delay((1000L / BREATH_SAMPLE_RATE_HZ).milliseconds)
        }
    }
        .scan(emptyList<Sample>()) { acc, s -> (acc + s).pruneOlderThan(DISPLAY_WINDOW_MS, s.tMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val displayWindowMs: Long = DISPLAY_WINDOW_MS

    val targetCycleLengthRange: StateFlow<ClosedFloatingPointRange<Float>> = breathingBusiness.targetCycleLengthRange
    val cycleLengthAllowedRange: ClosedFloatingPointRange<Float> = breathingBusiness.cycleLengthAllowedRange

    fun connect() = hrvRepository.connect()

    fun disconnect() = hrvRepository.disconnect()

    fun setTargetCycleLengthRange(range: ClosedFloatingPointRange<Float>) = breathingBusiness.setTargetCycleLengthRange(range)
}
