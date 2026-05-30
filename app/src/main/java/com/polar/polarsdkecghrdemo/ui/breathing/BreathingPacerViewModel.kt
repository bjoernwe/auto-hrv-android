package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingParams
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentConfig
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentHistoryUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.GenerateBreathingParamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DEFAULT_PARAMS = BreathingParams(outToInRatio = 2f, cycleLengthSeconds = 8f)

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    breathingPacerUseCase: BreathingPacerUseCase,
    generateBreathingParamsUseCase: GenerateBreathingParamsUseCase,
    private val historyUseCase: ExperimentHistoryUseCase,
) : ViewModel() {

    // Shared so currentParams and the recording coroutine consume the same upstream emissions.
    private val paramsFlow = generateBreathingParamsUseCase(ExperimentConfig.DEFAULT)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val currentParams: StateFlow<BreathingParams> =
        paramsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_PARAMS)

    val breathingState: Flow<BreathingState> = breathingPacerUseCase(viewModelScope, currentParams)

    init {
        viewModelScope.launch {
            var prev: BreathingParams? = null
            paramsFlow.collect { params ->
                prev?.let { historyUseCase.record(it) }
                prev = params
            }
        }
    }
}
