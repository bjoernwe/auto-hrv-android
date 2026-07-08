package dev.upaya.autohrv.domain.breathing

import dev.upaya.autohrv.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

sealed interface Exercise {
    val label: String

    suspend fun run(business: BreathingBusiness)

    data object SlowResonance : Exercise {
        override val label = "Slow resonance (8-12s)"
        val cycleLengthRange = 8..12

        override suspend fun run(business: BreathingBusiness) {
            business.setTargetCycleLengthRange(cycleLengthRange)
        }
    }

    data object NonResonance : Exercise {
        override val label = "Non-resonance (6-7s)"
        val cycleLengthRange = 6..7

        override suspend fun run(business: BreathingBusiness) {
            business.setTargetCycleLengthRange(cycleLengthRange)
        }
    }

    data object FastResonance : Exercise {
        override val label = "Fast resonance (4-5s)"
        val cycleLengthRange = 4..5

        override suspend fun run(business: BreathingBusiness) {
            business.setTargetCycleLengthRange(cycleLengthRange)
        }
    }

    data object Sweep : Exercise {
        override val label = "Sweep"

        // A full sweep (start -> fastest -> slowest -> start) always covers 2*(20-4)=32s of range,
        // so a constant speed gives even pacing on every leg regardless of the starting rate.
        val durationMs = 90_000L
        val tickMs = 100L
        val slowCap = 9

        override suspend fun run(business: BreathingBusiness) {
            val original = business.targetCycleLengthRange.value
            val allowed = business.cycleLengthAllowedRange.first..slowCap
            val start =
                business.currentBreathingPattern.value.cycleLengthSeconds
                    .coerceIn(allowed.first.toFloat(), allowed.last.toFloat())
            try {
                runSweep(business, start, allowed)
            } finally {
                business.setTargetCycleLengthRange(original)
            }
        }

        // Walks the swept center from `start` down to the fastest allowed rate, up to the
        // slowest, then back to `start`, over durationMs at a constant speed.
        private suspend fun runSweep(
            business: BreathingBusiness,
            start: Float,
            allowed: IntRange,
        ) {
            val fast = allowed.first.toFloat()
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
                    (centerRounded - 1).coerceIn(allowed)..(centerRounded + 1).coerceIn(allowed),
                )
                delay(tickMs.milliseconds)
            }
        }
    }

    companion object {
        val all: List<Exercise> = listOf(SlowResonance, NonResonance, FastResonance, Sweep)
    }
}

@Singleton
class Exercises
    @Inject
    internal constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
        private val business: BreathingBusiness,
    ) {
        private val _selectedExercise = MutableStateFlow<Exercise>(Exercise.SlowResonance)
        val selectedExercise: StateFlow<Exercise> = _selectedExercise

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private var job: Job? = null

        fun select(exercise: Exercise) {
            _selectedExercise.value = exercise
        }

        fun toggle() {
            if (job?.isActive == true) {
                job?.cancel()
            } else {
                job =
                    scope.launch {
                        _isRunning.value = true
                        try {
                            _selectedExercise.value.run(business)
                        } finally {
                            _isRunning.value = false
                        }
                    }
            }
        }
    }
