package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val breathingState: Flow<BreathingState> =
        breathingPacerUseCase(viewModelScope, _outToInRatio, _cycleLengthSeconds)

    fun setOutToInRatio(ratio: Float) {
        _outToInRatio.update { ratio }
    }

    fun setCycleLengthSeconds(seconds: Float) {
        _cycleLengthSeconds.update { seconds }
    }
}
