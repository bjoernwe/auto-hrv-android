package com.polar.polarsdkecghrdemo.domain.breathing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentHistoryUseCase @Inject constructor() {

    private val _history = MutableStateFlow<List<ExperimentRecord>>(emptyList())
    val history: StateFlow<List<ExperimentRecord>> = _history.asStateFlow()

    @Volatile private var latestPeriodicity: Float? = null

    fun updatePeriodicity(periodicity: Float) {
        latestPeriodicity = periodicity
    }

    fun record(params: BreathingParams) {
        val p = latestPeriodicity ?: return
        _history.update { it + ExperimentRecord(params, p) }
    }
}
