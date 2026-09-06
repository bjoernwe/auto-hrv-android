package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class ComputeBreathRrLagUseCase
    @Inject
    constructor() {

        /** Seconds by which the RR response lags behind the breath signal (positive = heart follows breath). */
        operator fun invoke(
            breathHistory: Flow<List<Float>>,
            rrHistory: Flow<List<Int>>,
            config: BreathingConfig,
        ): Flow<Float?> = combine(breathHistory, rrHistory) { breath, rr -> computeLag(breath, rr, config) }

        private fun computeLag(
            breath: List<Float>,
            rr: List<Int>,
            config: BreathingConfig,
        ): Float? {
            val n = minOf(breath.size, rr.size)
            if (n < 4) return null
            val b = breath.takeLast(n)
            val r = rr.takeLast(n)
            val bMean = b.average().toFloat()
            val rMean = r.average().toFloat()
            val bNorm = b.map { it - bMean }
            val rNorm = r.map { it.toFloat() - rMean }
            // RR is anti-phase to breath (HR rises on inhale → RR drops), so correlate breath vs –RR.
            // Peak at lag τ means the heart responds τ seconds after the breath signal.
            val maxLag = config.maxCycleLengthRange.last.coerceAtMost(n / 2)
            return (0..maxLag)
                .maxByOrNull { lag ->
                    (0 until n - lag).sumOf { t -> (bNorm[t] * (-rNorm[t + lag])).toDouble() }
                }?.toFloat()
        }
    }
