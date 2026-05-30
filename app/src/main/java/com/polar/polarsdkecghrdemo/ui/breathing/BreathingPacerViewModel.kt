package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingParams
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.GenerateBreathingParamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private val DEFAULT_PARAMS = BreathingParams(outToInRatio = 2f, cycleLengthSeconds = 8f)

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    breathingPacerUseCase: BreathingPacerUseCase,
    generateBreathingParamsUseCase: GenerateBreathingParamsUseCase,
) : ViewModel() {

    val currentParams: StateFlow<BreathingParams> = generateBreathingParamsUseCase(ExperimentConfig.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_PARAMS)

    val breathingState: Flow<BreathingState> = breathingPacerUseCase(viewModelScope, currentParams)
}
