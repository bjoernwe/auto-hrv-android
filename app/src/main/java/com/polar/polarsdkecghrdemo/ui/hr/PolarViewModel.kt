package com.polar.polarsdkecghrdemo.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HrUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val hr: Int? = null,
    val rrMs: List<Int> = emptyList(),
    val hrHistory: List<Int> = emptyList(),
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null,
)

class PolarViewModel(private val repository: PolarRepository) : ViewModel() {
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
            repository.getHrHistory(30).collect { _uiState.update { it } }
        }
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()

    class Factory(private val repository: PolarRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PolarViewModel::class.java)) {
                return PolarViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
