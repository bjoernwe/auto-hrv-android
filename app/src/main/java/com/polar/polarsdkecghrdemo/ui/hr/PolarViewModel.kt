package com.polar.polarsdkecghrdemo.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.experiment.ExperimentCoordinator
import com.polar.polarsdkecghrdemo.domain.experiment.TimeSeriesStats
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
    val stats: TimeSeriesStats? = null,
)

@HiltViewModel
class PolarViewModel @Inject constructor(
    private val repository: PolarRepository,
    private val coordinator: ExperimentCoordinator,
    polarRepository: PolarRepository,
) : ViewModel() {

    private val experimentConfig = ExperimentConfig.DEFAULT

    val deviceId: String = PolarRepository.DEVICE_ID

    private val _uiState = MutableStateFlow(HrUiState())
    val uiState: StateFlow<HrUiState> = _uiState.asStateFlow()

    init {
        val rrsMsHistory: Flow<List<Int>> = polarRepository.getRrsMsHistory(experimentConfig.experimentLengthSeconds)

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
            repository.hrResampled1Hz.collect { hr ->
                _uiState.update { it.copy(hr = hr) }
            }
        }
        viewModelScope.launch {
            rrsMsHistory.collect { history ->
                _uiState.update { it.copy(rrsMsHistory = history) }
            }
        }
        viewModelScope.launch {
            coordinator.stats.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()
}
