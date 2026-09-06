package dev.upaya.autohrv.domain.breathing

import dev.upaya.autohrv.domain.breathing.model.BreathingPatternBO

data class BreathingConfig(
    val acfWindowSeconds: Int,
    val acfMaxLagSeconds: Int,
    val acfHalfLifeSeconds: Float?,
    val inOutBias: Float,
    val initialCycleLength: Float,
    val maxCycleLengthRange: IntRange,
    val targetCycleLengthSmoothingWindow: Int,
    val resonancePeakToleranceSeconds: Float,
    val resonanceMinPeakValue: Float,
    val acfHistogramHalfLifeSeconds: Float?,
    val acfPcaHalfLifeSeconds: Float?,
    val acfPcaComponentCount: Int,
    val acfPcaMinObservations: Int,
) {
    init {
        require(acfMaxLagSeconds >= maxCycleLengthRange.last) {
            "acfMaxLagSeconds must cover the whole cycle-length search range"
        }
        // Needs headroom above the longest lag so the shortest-overlap estimate stays reliable.
        require(acfWindowSeconds >= acfMaxLagSeconds + 8) {
            "acfWindowSeconds must exceed acfMaxLagSeconds by the minimum overlap"
        }
        // The PCA runs on lags 1..acfMaxLagSeconds, so fewer curves than dimensions leaves the
        // covariance rank-deficient and its trailing components pure noise.
        require(acfPcaMinObservations > acfMaxLagSeconds) {
            "acfPcaMinObservations must exceed the number of lags entering the PCA"
        }
    }

    companion object {
        val DEFAULT =
            BreathingConfig(
                // 1 Hz history length; recency half-life ~12 s keeps the ACF as responsive as the
                // rest of the main screen while retaining enough support for the longest lag.
                acfWindowSeconds = 30,
                acfMaxLagSeconds = 16,
                acfHalfLifeSeconds = 12f,
                inOutBias = 0f,
                initialCycleLength = 9f,
                maxCycleLengthRange = 3..16,
                targetCycleLengthSmoothingWindow = 40,
                resonancePeakToleranceSeconds = 1.5f,
                resonanceMinPeakValue = 0.35f,
                // Old peaks fade over ~1.5 min so the histogram tracks the recent session.
                acfHistogramHalfLifeSeconds = 90f,
                // The components describe the session rather than the moment, so they decay far
                // more slowly than the histogram — ~10 min keeps them stable while still letting
                // them follow a genuine change of state.
                acfPcaHalfLifeSeconds = 600f,
                acfPcaComponentCount = 3,
                acfPcaMinObservations = 60,
            )
    }
}

fun BreathingConfig.defaultPattern() = BreathingPatternBO(inOutBias, initialCycleLength)
