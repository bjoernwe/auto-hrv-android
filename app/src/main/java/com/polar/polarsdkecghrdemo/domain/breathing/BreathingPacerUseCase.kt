package com.polar.polarsdkecghrdemo.domain.breathing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import javax.inject.Inject

enum class BreathingPhase { Inhale, Exhale }

data class BreathingState(val phase: BreathingPhase, val progress: Float)

data class BreathingParams(val outToInRatio: Float, val cycleLengthSeconds: Float)

class BreathingPacerUseCase @Inject constructor() {

    operator fun invoke(scope: CoroutineScope, params: StateFlow<BreathingParams>): Flow<BreathingState> =
        cycleFlow(params).shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private fun cycleFlow(params: StateFlow<BreathingParams>): Flow<BreathingState> = flow {
        while (currentCoroutineContext().isActive) {
            // Snapshot params once per cycle so in-flight cycles are never interrupted
            val (outToInRatio, cycleLengthSeconds) = params.value
            val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
            val inhaleMs = (cycleMs / (1.0 + outToInRatio)).toLong().coerceAtLeast(200L)
            val exhaleMs = (cycleMs - inhaleMs).coerceAtLeast(200L)

            val cycleStart = System.currentTimeMillis()
            while (currentCoroutineContext().isActive) {
                val elapsed = System.currentTimeMillis() - cycleStart
                if (elapsed >= inhaleMs + exhaleMs) break
                if (elapsed < inhaleMs) {
                    emit(BreathingState(BreathingPhase.Inhale, elapsed.toFloat() / inhaleMs))
                } else {
                    emit(BreathingState(BreathingPhase.Exhale, (elapsed - inhaleMs).toFloat() / exhaleMs))
                }
                delay(16L)
            }
        }
    }
}
