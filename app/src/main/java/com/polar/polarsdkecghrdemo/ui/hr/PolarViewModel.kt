package com.polar.polarsdkecghrdemo.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord
import com.polar.polarsdkecghrdemo.domain.experiment.ExperimentCoordinator
import com.polar.polarsdkecghrdemo.domain.hr.CalcHrStatsUseCase
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
    val rrsMsHistory: List<Int> = emptyList(),
    val batteryLevel: Int? = null,
    val smoothness: Float? = null,
    val powerSpectrum: List<Float>? = null,
    val periodicity: Float? = null,
    val experimentHistory: List<ExperimentRecord> = emptyList(),
)

@HiltViewModel
class PolarViewModel @Inject constructor(
    private val repository: PolarRepository,
    private val calcHrStatsUseCase: CalcHrStatsUseCase,
    private val coordinator: ExperimentCoordinator,
) : ViewModel() {
    val deviceId: String = PolarRepository.DEVICE_ID

    private val _uiState = MutableStateFlow(HrUiState())
    val uiState: StateFlow<HrUiState> = _uiState.asStateFlow()

    init {
        val rrsMsHistory = coordinator.rrsMsHistory

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
            rrsMsHistory.collect { history ->
                _uiState.update { it.copy(rrsMsHistory = history) }
            }
        }
        viewModelScope.launch {
            calcHrStatsUseCase.smoothness(rrsMsHistory).collect { smoothness ->
                _uiState.update { it.copy(smoothness = smoothness) }
            }
        }
        viewModelScope.launch {
            calcHrStatsUseCase.powerSpectrum(rrsMsHistory).collect { spectrum ->
                _uiState.update { it.copy(powerSpectrum = spectrum) }
            }
        }
        viewModelScope.launch {
            coordinator.periodicity.collect { p ->
                _uiState.update { it.copy(periodicity = p) }
            }
        }
        viewModelScope.launch {
            coordinator.experimentRecords.collect { history ->
                _uiState.update { it.copy(experimentHistory = history) }
            }
        }
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()
}
