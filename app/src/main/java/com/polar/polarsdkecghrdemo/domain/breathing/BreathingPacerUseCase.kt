package com.polar.polarsdkecghrdemo.domain.breathing

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

enum class BreathingPhase { Inhale, Exhale }

data class BreathingState(
    val phase: BreathingPhase,
    val progress: Float,
)

class BreathingPacerUseCase @Inject constructor() {
    operator fun invoke(outToInRatio: Float, cycleLengthSeconds: Float): Flow<BreathingState> = flow {
        val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
        val inhaleMs = (cycleMs / (1.0 + outToInRatio)).toLong().coerceAtLeast(100L)
        val exhaleMs = (cycleMs - inhaleMs).coerceAtLeast(100L)

        while (currentCoroutineContext().isActive) {
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
