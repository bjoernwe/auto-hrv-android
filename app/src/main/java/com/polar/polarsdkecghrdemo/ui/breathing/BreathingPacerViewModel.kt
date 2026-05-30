package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import com.polar.polarsdkecghrdemo.domain.experiment.ExperimentCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    breathingPacerUseCase: BreathingPacerUseCase,
    coordinator: ExperimentCoordinator,
) : ViewModel() {

    val currentParams = coordinator.currentBreathingPattern

    val breathingState: Flow<BreathingState> =
        breathingPacerUseCase(viewModelScope, coordinator.currentBreathingPattern)
}
