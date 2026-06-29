package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos

enum class BreathingPhase { Inhale, Exhale }

data class BreathingPattern(
    val outToInRatio: Float,
    val cycleLengthSeconds: Float,
) {
    operator fun plus(other: BreathingPattern) = BreathingPattern(
        outToInRatio + other.outToInRatio,
        cycleLengthSeconds + other.cycleLengthSeconds,
    )
    operator fun div(x: Float) = BreathingPattern(outToInRatio / x, cycleLengthSeconds / x)
}

data class BreathingPhaseStart(
    val phase: BreathingPhase,
    val startTimeMs: Long,
    val durationMs: Long,
) {
    fun valueAt(nowMs: Long): Float {
        val progress = ((nowMs - startTimeMs).toFloat() / durationMs).coerceIn(0f, 1f)
        return when (phase) {
            BreathingPhase.Inhale -> 0.5f - 0.5f * cos(PI.toFloat() * progress)
            BreathingPhase.Exhale -> 0.5f + 0.5f * cos(PI.toFloat() * progress)
        }
    }
}

class PacerOutput(
    val currentPhaseStart: StateFlow<BreathingPhaseStart>,
    val currentPattern: StateFlow<BreathingPattern>,
)

class BreathingPacerUseCase @Inject constructor() {

    operator fun invoke(scope: CoroutineScope, targetPattern: StateFlow<BreathingPattern>): PacerOutput {
        val currentBreathingPattern = MutableStateFlow(targetPattern.value)

        val currentPhaseStart = flow {
            while (true) {
                val inhalePattern = targetPattern.value
                currentBreathingPattern.value = inhalePattern
                val inhaleMs = inhalePattern.inhaleMs()
                emit(BreathingPhaseStart(BreathingPhase.Inhale, System.currentTimeMillis(), inhaleMs))
                delay(inhaleMs)

                val exhalePattern = targetPattern.value
                currentBreathingPattern.value = exhalePattern
                val exhaleMs = exhalePattern.exhaleMs()
                emit(BreathingPhaseStart(BreathingPhase.Exhale, System.currentTimeMillis(), exhaleMs))
                delay(exhaleMs)
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BreathingPhaseStart(
                BreathingPhase.Inhale,
                System.currentTimeMillis(),
                targetPattern.value.inhaleMs(),
            ),
        )

        return PacerOutput(currentPhaseStart, currentBreathingPattern.asStateFlow())
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
