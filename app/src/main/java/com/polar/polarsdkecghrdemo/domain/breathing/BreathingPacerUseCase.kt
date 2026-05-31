package com.polar.polarsdkecghrdemo.domain.breathing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class BreathingPhase { Inhale, Exhale }

data class BreathingState(val phase: BreathingPhase, val progress: Float, val pattern: BreathingPattern)

data class BreathingPattern(val outToInRatio: Float, val cycleLengthSeconds: Float)

class BreathingPacerUseCase @Inject constructor() {

    operator fun invoke(scope: CoroutineScope, targetPattern: StateFlow<BreathingPattern>): StateFlow<BreathingState> {
        return flow {
            while (true) {
                val inhalePattern = targetPattern.value
                emitAll(progressFlow(inhalePattern.inhaleMs()).map { BreathingState(BreathingPhase.Inhale, it, inhalePattern) })
                val exhalePattern = targetPattern.value
                emitAll(progressFlow(exhalePattern.exhaleMs()).map { BreathingState(BreathingPhase.Exhale, it, exhalePattern) })
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BreathingState(BreathingPhase.Inhale, 0f, targetPattern.value)
        )
    }

    private fun progressFlow(durationMs: Long): Flow<Float> = flow {
        val start = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            if (elapsed >= durationMs) break
            emit((elapsed.toFloat() / durationMs).coerceAtMost(1f))
            delay(16L)
        }
    }
}

private fun BreathingPattern.inhaleMs(): Long {
    val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
    return (cycleMs / (1.0 + outToInRatio)).toLong().coerceAtLeast(200L)
}

private fun BreathingPattern.exhaleMs(): Long {
    val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
    return (cycleMs - inhaleMs()).coerceAtLeast(200L)
}
