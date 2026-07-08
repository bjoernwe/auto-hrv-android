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

    data object SlowResonance : Exercise {
        override val label = "Slow resonance"
    }

    data object FastResonance : Exercise {
        override val label = "Fast resonance"
    }

    data object Sweep : Exercise {
        override val label = "Sweep"
    }
}

// A full sweep (start -> fastest -> slowest -> start) always covers 2*(20-4)=32s of range,
// so a constant speed gives even pacing on every leg regardless of the starting rate.
private const val SWEEP_DURATION_MS = 90_000L
private const val SWEEP_TICK_MS = 100L

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
            if (_isRunning.value) job?.cancel() else run(_selectedExercise.value)
        }

        private fun run(exercise: Exercise) {
            when (exercise) {
                Exercise.SlowResonance -> business.setTargetCycleLengthRange(8..12)
                Exercise.FastResonance -> business.setTargetCycleLengthRange(4..6)
                Exercise.Sweep -> startSweep()
            }
        }

        private fun startSweep() {
            val original = business.targetCycleLengthRange.value
            val allowed = business.cycleLengthAllowedRange.first..9
            val start =
                business.currentBreathingPattern.value.cycleLengthSeconds
                    .coerceIn(allowed.first.toFloat(), allowed.last.toFloat())
            _isRunning.value = true
            job =
                scope.launch {
                    try {
                        runSweep(start, allowed)
                    } finally {
                        business.setTargetCycleLengthRange(original)
                        _isRunning.value = false
                    }
                }
        }

        // Walks the swept center from `start` down to the fastest allowed rate, up to the
        // slowest, then back to `start`, over SWEEP_DURATION_MS at a constant speed.
        private suspend fun runSweep(
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
                if (elapsed >= SWEEP_DURATION_MS) break
                val distanceTravelled = totalDistance * (elapsed.toFloat() / SWEEP_DURATION_MS)
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
                delay(SWEEP_TICK_MS.milliseconds)
            }
        }
    }
