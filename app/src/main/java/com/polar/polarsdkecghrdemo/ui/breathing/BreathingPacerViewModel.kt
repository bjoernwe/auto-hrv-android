package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.experiment.ExperimentCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    breathingPacerUseCase: BreathingPacerUseCase,
    coordinator: ExperimentCoordinator,
) : ViewModel() {

    val currentBreathingPattern: StateFlow<BreathingPattern> = coordinator.currentBreathingPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExperimentConfig.DEFAULT.defaultParams())

    val breathingState: StateFlow<BreathingState> =
        breathingPacerUseCase(viewModelScope, currentBreathingPattern)
}
