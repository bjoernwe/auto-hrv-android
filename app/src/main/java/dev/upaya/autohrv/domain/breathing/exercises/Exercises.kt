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
        private val _activeExercise = MutableStateFlow<Exercise?>(null)
        val activeExercise: StateFlow<Exercise?> = _activeExercise

        private var job: Job? = null

        init {
            select(FreeRange)
        }

        fun select(exercise: Exercise) {
            val previous = job
            job =
                scope.launch {
                    // Let the previous run's `finally` complete before we take ownership of
                    // `_activeExercise`, otherwise a stale cancellation cleanup can clobber it
                    // back to null after we've set it to the new exercise.
                    previous?.cancelAndJoin()
                    _activeExercise.value = exercise
                    try {
                        exercise.run(business)
                    } finally {
                        _activeExercise.value = null
                    }
                }
        }
    }
