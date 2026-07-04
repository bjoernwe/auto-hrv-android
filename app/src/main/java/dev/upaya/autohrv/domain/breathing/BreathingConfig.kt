package dev.upaya.autohrv.domain.breathing

data class BreathingConfig(
    val acfWindowSeconds: Int,
    val acfMaxLagSeconds: Int,
    val acfHalfLifeSeconds: Float?,
    val outToInRatio: Float,
    val initialCycleLength: Float,
    val initialCycleLengthRange: ClosedFloatingPointRange<Float>,
    val maxCycleLengthRange: ClosedFloatingPointRange<Float>,
    val targetCycleLengthSmoothingWindow: Int,
    val resonancePeakToleranceSeconds: Float,
    val resonanceMinPeakValue: Float,
    val windowLength: Int,
    val acfHistogramExpGain: Float,
    val acfHistogramSigmoidSteepness: Float,
    val acfHistogramSigmoidMidpoint: Float,
    val acfHistogramHalfLifeSeconds: Float?,
    val acfHistogramIgnoredLeadingLags: Int,
) {
    init {
        require(acfMaxLagSeconds >= maxCycleLengthRange.endInclusive) {
            "acfMaxLagSeconds must cover the whole cycle-length search range"
        }
        // Needs headroom above the longest lag so the shortest-overlap estimate stays reliable.
        require(acfWindowSeconds >= acfMaxLagSeconds + 8) {
            "acfWindowSeconds must exceed acfMaxLagSeconds by the minimum overlap"
        }
    }

    companion object {
        val DEFAULT =
            BreathingConfig(
                // 1 Hz history length; recency half-life ~12 s keeps the ACF as responsive as the
                // rest of the main screen while retaining enough support for the longest lag.
                acfWindowSeconds = 35,
                acfMaxLagSeconds = 20,
                acfHalfLifeSeconds = 12f,
                outToInRatio = 1f,
                initialCycleLength = 8f,
                initialCycleLengthRange = 6f..10f,
                maxCycleLengthRange = 4f..20f,
                targetCycleLengthSmoothingWindow = 40,
                resonancePeakToleranceSeconds = 1.5f,
                resonanceMinPeakValue = 0.35f,
                windowLength = 20,
                // Shaping of the accumulated ACF histogram: exp sharpens the dominant lags, the
                // closing sigmoid keeps secondary peaks visible. Tune on-device.
                acfHistogramExpGain = 2.5f,
                acfHistogramSigmoidSteepness = 5f,
                acfHistogramSigmoidMidpoint = 0.2f,
                // Old peaks fade over ~1.5 min so the histogram tracks the recent session.
                acfHistogramHalfLifeSeconds = 90f,
                // "Ignore up to lag 3": lags 1-3 are naturally high (short-lag autocorrelation) and
                // would otherwise dominate the shaping normalization, squashing the real
                // breathing-range peaks. Lag 0 is always ignored on top of this. They're capped
                // into range rather than dropped, so they still show in the histogram.
                acfHistogramIgnoredLeadingLags = 3,
            )
    }
}

fun BreathingConfig.defaultPattern() = BreathingPattern(outToInRatio, initialCycleLength)
