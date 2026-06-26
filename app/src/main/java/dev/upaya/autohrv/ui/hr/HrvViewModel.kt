package dev.upaya.autohrv.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.upaya.autohrv.data.model.ConnectionState
import dev.upaya.autohrv.data.repository.HrvRepository
import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HrUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val hr: Int? = null,
    val rrsMsHistory: List<Int> = emptyList(),
    val batteryLevel: Int? = null,
    val rmssd: Float? = null,
    val autoCorrelation: List<Float>? = null,
    val autoCorrelationPeak: Float? = null,
    val isInResonance: Boolean = false,
)

const val AUTO_CORRELATION_SIZE = 20

@HiltViewModel
class HrvViewModel @Inject constructor(
    private val hrvRepository: HrvRepository,
    private val breathingBusiness: BreathingBusiness,
) : ViewModel() {

    private val breathingConfig = BreathingConfig.DEFAULT

    val deviceId: String = HrvRepository.DEVICE_ID

    private val _uiState = MutableStateFlow(HrUiState())
    val uiState: StateFlow<HrUiState> = _uiState.asStateFlow()

    init {
        val rrsMsHistory: Flow<List<Int>> = hrvRepository.getRrsMsHistory(breathingConfig.evaluationLengthSeconds)

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
            rrsMsHistory.collect { history ->
                _uiState.update { it.copy(rrsMsHistory = history) }
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

    val breathingState: StateFlow<BreathingState> = breathingBusiness.currentBreathingState
    val currentPattern: StateFlow<BreathingPattern> = breathingBusiness.currentBreathingPattern
    val targetOutToInRatio: StateFlow<Float> = breathingBusiness.targetOutToInRatio
    val targetCycleLengthRange: StateFlow<ClosedFloatingPointRange<Float>> = breathingBusiness.targetCycleLengthRange
    val cycleLengthAllowedRange: ClosedFloatingPointRange<Float> = breathingBusiness.cycleLengthAllowedRange

    fun connect() = hrvRepository.connect()

    fun disconnect() = hrvRepository.disconnect()

    fun setTargetOutToInRatio(ratio: Float) = breathingBusiness.setTargetOutToInRatio(ratio)

    fun setTargetCycleLengthRange(range: ClosedFloatingPointRange<Float>) = breathingBusiness.setTargetCycleLengthRange(range)
}
