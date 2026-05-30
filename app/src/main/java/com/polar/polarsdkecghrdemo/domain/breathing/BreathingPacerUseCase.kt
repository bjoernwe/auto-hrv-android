package com.polar.polarsdkecghrdemo.domain.breathing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
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
            // Re-snapshot at each breath boundary so param changes take effect between breaths
            emitAll(phaseFlow(BreathingPhase.Inhale, params.value.inhaleMs()))
            emitAll(phaseFlow(BreathingPhase.Exhale, params.value.exhaleMs()))
        }
    }

    private fun phaseFlow(phase: BreathingPhase, durationMs: Long): Flow<BreathingState> = flow {
        val start = System.currentTimeMillis()
        while (currentCoroutineContext().isActive) {
            val elapsed = System.currentTimeMillis() - start
            if (elapsed >= durationMs) break
            emit(BreathingState(phase, elapsed.toFloat() / durationMs))
            delay(16L)
        }
    }

    private fun BreathingParams.inhaleMs(): Long {
        val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
        return (cycleMs / (1.0 + outToInRatio)).toLong().coerceAtLeast(200L)
    }

    private fun BreathingParams.exhaleMs(): Long {
        val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
        return (cycleMs - inhaleMs()).coerceAtLeast(200L)
    }
}
