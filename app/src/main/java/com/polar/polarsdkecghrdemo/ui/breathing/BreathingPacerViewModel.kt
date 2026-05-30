package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.lifecycle.ViewModel
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPacerUseCase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class BreathingPacerViewModel @Inject constructor(
    private val breathingPacerUseCase: BreathingPacerUseCase,
) : ViewModel() {

    private val _outToInRatio = MutableStateFlow(2f)
    val outToInRatio: StateFlow<Float> = _outToInRatio.asStateFlow()

    private val _cycleLengthSeconds = MutableStateFlow(8f)
    val cycleLengthSeconds: StateFlow<Float> = _cycleLengthSeconds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val breathingState: StateFlow<BreathingState?> =
        combine(_outToInRatio, _cycleLengthSeconds) { ratio, length -> ratio to length }
            .flatMapLatest { (ratio, length) -> breathingPacerUseCase(ratio, length) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setOutToInRatio(ratio: Float) {
        _outToInRatio.value = ratio
    }

    fun setCycleLengthSeconds(seconds: Float) {
        _cycleLengthSeconds.value = seconds
    }
}
