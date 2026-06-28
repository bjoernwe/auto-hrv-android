package dev.upaya.autohrv.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.upaya.autohrv.data.model.ConnectionState
import dev.upaya.autohrv.data.repository.HrvRepository
import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingPhaseStart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
)

const val AUTO_CORRELATION_SIZE = 20

private const val BREATH_SAMPLE_RATE_HZ = 20
private const val DISPLAY_WINDOW_MS = 22_000L

@HiltViewModel
class HrvViewModel @Inject constructor(
    private val hrvRepository: HrvRepository,
    private val breathingBusiness: BreathingBusiness,
) : ViewModel() {

    private val breathingConfig = BreathingConfig.DEFAULT

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
                _uiState.update { it.copy(
                    rmssd = stats?.beatRrsStats?.rmssd,
                    autoCorrelation = stats?.resampledRrsStats?.autoCorrelation?.takeIf { it.size >= AUTO_CORRELATION_SIZE }?.take(AUTO_CORRELATION_SIZE),
                    autoCorrelationPeak = peak,
                ) }
            }
        }
        viewModelScope.launch {
            breathingBusiness.isInResonance.collect { isInResonance ->
                _uiState.update { it.copy(isInResonance = isInResonance) }
            }
        }
    }

    val currentPhaseStart: StateFlow<BreathingPhaseStart> = breathingBusiness.currentPhaseStart
    val currentPattern: StateFlow<BreathingPattern> = breathingBusiness.currentBreathingPattern
    val targetOutToInRatio: StateFlow<Float> = breathingBusiness.targetOutToInRatio

    /** Pacer function sampled at 20 Hz, each point stamped with real wall-clock time. */
    val breathSamples: StateFlow<List<Sample>> = flow {
        while (true) {
            val t = System.currentTimeMillis()
            emit(Sample(t, breathingBusiness.currentPhaseStart.value.valueAt(t)))
            delay(1000L / BREATH_SAMPLE_RATE_HZ)
        }
    }
        .scan(emptyList<Sample>()) { acc, s -> (acc + s).pruneOlderThan(DISPLAY_WINDOW_MS, s.tMillis) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val displayWindowMs: Long = DISPLAY_WINDOW_MS

    val targetCycleLengthRange: StateFlow<ClosedFloatingPointRange<Float>> = breathingBusiness.targetCycleLengthRange
    val cycleLengthAllowedRange: ClosedFloatingPointRange<Float> = breathingBusiness.cycleLengthAllowedRange

    fun connect() = hrvRepository.connect()

    fun disconnect() = hrvRepository.disconnect()

    fun setTargetOutToInRatio(ratio: Float) = breathingBusiness.setTargetOutToInRatio(ratio)

    fun setTargetCycleLengthRange(range: ClosedFloatingPointRange<Float>) = breathingBusiness.setTargetCycleLengthRange(range)
}
