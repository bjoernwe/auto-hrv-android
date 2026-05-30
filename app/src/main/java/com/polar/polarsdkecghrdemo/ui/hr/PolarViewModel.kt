package com.polar.polarsdkecghrdemo.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HrUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val hr: Int? = null,
    val rrMs: List<Int> = emptyList(),
    val hrHistory: List<Int> = emptyList(),
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null,
)

@HiltViewModel
class PolarViewModel @Inject constructor(private val repository: PolarRepository) : ViewModel() {
    val deviceId: String = PolarRepository.DEVICE_ID

    private val _uiState = MutableStateFlow(HrUiState())
    val uiState: StateFlow<HrUiState> = _uiState.asStateFlow()

    init {
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
            repository.firmwareVersion.collect { version ->
                _uiState.update { it.copy(firmwareVersion = version) }
            }
        }
        viewModelScope.launch {
            repository.hrFlow.collect { sample ->
                _uiState.update { current ->
                    current.copy(
                        hr = sample.hr,
                        rrMs = sample.rrsMs.ifEmpty { current.rrMs },
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.getHrHistory(30).collect {  history ->
                _uiState.update { it.copy(hrHistory = history) }
            }
        }
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()


}
