package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingParams
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    private val breathingPacerUseCase: BreathingPacerUseCase,
) : ViewModel() {

    private val _outToInRatio = MutableStateFlow(2f)
    val outToInRatio: StateFlow<Float> = _outToInRatio.asStateFlow()

    private val _cycleLengthSeconds = MutableStateFlow(8f)
    val cycleLengthSeconds: StateFlow<Float> = _cycleLengthSeconds.asStateFlow()

    // Params StateFlow is passed into the use case so it can sample .value at each cycle
    // boundary — changes take effect at the start of the next cycle, never mid-cycle.
    private val params: StateFlow<BreathingParams> =
        combine(_outToInRatio, _cycleLengthSeconds) { ratio, length ->
            BreathingParams(ratio, length)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            BreathingParams(_outToInRatio.value, _cycleLengthSeconds.value),
        )

    val breathingState: StateFlow<BreathingState?> =
        breathingPacerUseCase(params)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setOutToInRatio(ratio: Float) {
        _outToInRatio.update { ratio }
    }

    fun setCycleLengthSeconds(seconds: Float) {
        _cycleLengthSeconds.update { seconds }
    }
}
