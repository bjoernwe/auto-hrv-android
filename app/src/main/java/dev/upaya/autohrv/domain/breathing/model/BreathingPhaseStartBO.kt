package dev.upaya.autohrv.domain.breathing.model

import kotlin.math.PI
import kotlin.math.cos

data class BreathingPhaseStartBO(
    val phase: BreathingPhaseBO,
    val startTimeMs: Long,
    val durationMs: Long,
) {
    fun valueAt(nowMs: Long): Float {
        val progress = ((nowMs - startTimeMs).toFloat() / durationMs).coerceIn(0f, 1f)
        return when (phase) {
            BreathingPhaseBO.Inhale -> 0.5f - 0.5f * cos(PI.toFloat() * progress)
            BreathingPhaseBO.Exhale -> 0.5f + 0.5f * cos(PI.toFloat() * progress)
        }
    }
}
