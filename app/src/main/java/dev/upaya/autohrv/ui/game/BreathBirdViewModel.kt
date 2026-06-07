package dev.upaya.autohrv.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BreathBirdViewModel @Inject constructor(
    coordinator: BreathingBusiness,
) : ViewModel() {

    val periodSec: StateFlow<Float> = coordinator.currentBreathingPattern
        .map { it.cycleLengthSeconds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 10f)

    val hrvQuality: StateFlow<Float> = coordinator.stats
        .map { stats ->
            val rmssd = stats?.beatRrsStats?.rmssd ?: return@map 0.85f
            ((rmssd - 20f) / (80f - 20f)).coerceIn(0.2f, 1.0f)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.85f)
}
