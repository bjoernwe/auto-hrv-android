package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.di.ApplicationScope
import dev.upaya.autohrv.domain.breathing.model.BreathingPatternBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPhaseBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPhaseStartBO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.pow

class PacerOutput(
    val currentPhaseStart: StateFlow<BreathingPhaseStartBO>,
    val currentPattern: StateFlow<BreathingPatternBO>,
)

class RunBreathingPacerUseCase
    @Inject
    constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {

        operator fun invoke(targetPattern: StateFlow<BreathingPatternBO>): PacerOutput {
            val currentBreathingPattern = MutableStateFlow(targetPattern.value)

            val currentPhaseStart =
                flow {
                    while (true) {
                        val inhalePattern = targetPattern.value
                        currentBreathingPattern.value = inhalePattern
                        val inhaleMs = inhalePattern.inhaleMs()
                        emit(BreathingPhaseStartBO(BreathingPhaseBO.Inhale, System.currentTimeMillis(), inhaleMs))
                        delay(inhaleMs)

                        val exhalePattern = targetPattern.value
                        currentBreathingPattern.value = exhalePattern
                        val exhaleMs = exhalePattern.exhaleMs()
                        emit(BreathingPhaseStartBO(BreathingPhaseBO.Exhale, System.currentTimeMillis(), exhaleMs))
                        delay(exhaleMs)
                    }
                }.stateIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue =
                        BreathingPhaseStartBO(
                            BreathingPhaseBO.Inhale,
                            System.currentTimeMillis(),
                            targetPattern.value.inhaleMs(),
                        ),
                )

            return PacerOutput(currentPhaseStart, currentBreathingPattern.asStateFlow())
        }
    }

private fun BreathingPatternBO.outToInRatio(): Float = 2f.pow(bias.coerceIn(-1f, 1f))

private fun BreathingPatternBO.inhaleMs(): Long {
    val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
    return (cycleMs / (1.0 + outToInRatio())).toLong().coerceAtLeast(200L)
}

private fun BreathingPatternBO.exhaleMs(): Long {
    val cycleMs = (cycleLengthSeconds * 1000.0).toLong()
    return (cycleMs - inhaleMs()).coerceAtLeast(200L)
}
