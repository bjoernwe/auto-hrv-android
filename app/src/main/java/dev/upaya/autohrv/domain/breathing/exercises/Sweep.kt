package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

data object Sweep : Exercise {
    override val label = "sweep"

    // A full sweep (start -> fastest -> slowest -> start) always covers 2*(20-4)=32s of range,
    // so a constant speed gives even pacing on every leg regardless of the starting rate.
    val durationMs = 300_000L
    val tickMs = 100L
    val slowCap = 13

    override suspend fun run(business: BreathingBusiness) {
        val original = business.targetCycleLengthRange.value
        val allowed = business.cycleLengthAllowedRange.first..slowCap
        val start =
            business.currentBreathingPattern.value.cycleLengthSeconds
                .coerceIn(allowed.first.toFloat(), allowed.last.toFloat())
        try {
            runSweep(business, start, allowed)
            business.setTargetCycleLengthRange(original)
        } finally {
            // just stop and keep current range
        }
    }

    // Walks the swept center from `start` down to the fastest allowed rate, up to the
    // slowest, then back to `start`, over durationMs at a constant speed.
    private suspend fun runSweep(
        business: BreathingBusiness,
        start: Float,
        allowed: IntRange,
    ) {
        val fast = allowed.first.minus(1).toFloat()
        val slow = allowed.last.toFloat()
        val legToFast = start - fast
        val legToSlow = slow - fast
        val legToStart = slow - start
        val totalDistance = legToFast + legToSlow + legToStart
        if (totalDistance <= 0f) return
        val t0 = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - t0
            if (elapsed >= durationMs) break
            val distanceTravelled = totalDistance * (elapsed.toFloat() / durationMs)
            val center =
                when {
                    distanceTravelled <= legToFast -> start - distanceTravelled
                    distanceTravelled <= legToFast + legToSlow -> fast + (distanceTravelled - legToFast)
                    else -> slow - (distanceTravelled - legToFast - legToSlow)
                }
            val centerRounded = center.roundToInt()
            business.setTargetCycleLengthRange(
                // (centerRounded - 1..centerRounded,
                centerRounded..centerRounded,
            )
            delay(tickMs.milliseconds)
        }
    }
}
