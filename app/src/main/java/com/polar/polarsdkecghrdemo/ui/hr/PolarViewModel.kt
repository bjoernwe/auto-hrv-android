package com.polar.polarsdkecghrdemo.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.domain.hr.HeartRateStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HrUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val hr: Int? = null,
    val hrHistory: List<Int> = emptyList(),
    val batteryLevel: Int? = null,
    val smoothness: Float? = null,
)

@HiltViewModel
class PolarViewModel @Inject constructor(
    private val repository: PolarRepository,
    private val heartRateStatsUseCase: HeartRateStatsUseCase,
) : ViewModel() {
    val deviceId: String = PolarRepository.DEVICE_ID

    private val _uiState = MutableStateFlow(HrUiState())
    val uiState: StateFlow<HrUiState> = _uiState.asStateFlow()

    init {
        val hrHistory = repository.getHrHistory(30)
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
        viewModelScope.launch {
            repository.batteryLevel.collect { level ->
                _uiState.update { it.copy(batteryLevel = level) }
            }
        }
        viewModelScope.launch {
            repository.hrFlow.collect { sample ->
                _uiState.update { it.copy(hr = sample.hr) }
            }
        }
        viewModelScope.launch {
            hrHistory.collect { history ->
                _uiState.update { it.copy(hrHistory = history) }
            }
        }
        viewModelScope.launch {
            heartRateStatsUseCase(hrHistory).collect { smoothness ->
                _uiState.update { it.copy(smoothness = smoothness) }
            }
        }
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()
}
