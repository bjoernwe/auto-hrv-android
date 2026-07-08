package dev.upaya.autohrv.domain.breathing.exercises

import dev.upaya.autohrv.di.ApplicationScope
import dev.upaya.autohrv.domain.breathing.BreathingBusiness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Exercises
    @Inject
    internal constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
        private val business: BreathingBusiness,
    ) {
        private val _selectedExercise = MutableStateFlow<Exercise>(FreeRange)
        val selectedExercise: StateFlow<Exercise> = _selectedExercise

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private var job: Job? = null

        fun select(exercise: Exercise) {
            _selectedExercise.value = exercise
            start()
        }

        private fun stop() {
            job?.cancel()
        }

        private fun start() {
            val previous = job
            job =
                scope.launch {
                    // Let the previous run's `finally` complete before we take ownership of
                    // `_isRunning`, otherwise a stale cancellation cleanup can clobber it back
                    // to false after we've set it true.
                    previous?.cancelAndJoin()
                    _isRunning.value = true
                    try {
                        _selectedExercise.value.run(business)
                    } finally {
                        _isRunning.value = false
                    }
                }
        }

        fun toggle() {
            if (job?.isActive == true) {
                stop()
            } else {
                start()
            }
        }
    }
