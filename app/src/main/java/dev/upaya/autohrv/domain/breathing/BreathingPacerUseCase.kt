package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class BreathingPhase { Inhale, Exhale }

data class BreathingState(val phase: BreathingPhase, val progress: Float)

data class BreathingPattern(
    val outToInRatio: Float,
    val cycleLengthSeconds: Float,
) {
    companion object {
        val DEFAULT = BreathingPattern(
            ExperimentConfig.DEFAULT.outToInRatioMean,
            ExperimentConfig.DEFAULT.cycleLengthMean,
        )
    }

    operator fun plus(other: BreathingPattern) = BreathingPattern(
        outToInRatio + other.outToInRatio,
        cycleLengthSeconds + other.cycleLengthSeconds
    )

    operator fun div(x: Float) = BreathingPattern(
        outToInRatio / x,
        cycleLengthSeconds / x
    )
}

class PacerOutput(
    val breathingState: StateFlow<BreathingState>,
    val currentPattern: StateFlow<BreathingPattern>,
)

class BreathingPacerUseCase @Inject constructor() {

    operator fun invoke(scope: CoroutineScope, targetPattern: StateFlow<BreathingPattern>): PacerOutput {
        val currentBreathingPattern = MutableStateFlow(targetPattern.value)

        val breathingState = flow {
            while (true) {
                val inhalePattern = targetPattern.value
                currentBreathingPattern.value = inhalePattern
                emitAll(progressFlow(inhalePattern.inhaleMs()).map { BreathingState(BreathingPhase.Inhale, it) })
                val exhalePattern = targetPattern.value
                currentBreathingPattern.value = exhalePattern
                emitAll(progressFlow(exhalePattern.exhaleMs()).map { BreathingState(BreathingPhase.Exhale, it) })
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BreathingState(BreathingPhase.Inhale, 0f)
        )

        return PacerOutput(breathingState, currentBreathingPattern.asStateFlow())
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
