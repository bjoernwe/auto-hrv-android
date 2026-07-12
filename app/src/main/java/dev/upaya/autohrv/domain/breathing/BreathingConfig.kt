package dev.upaya.autohrv.domain.breathing

data class BreathingConfig(
    val acfWindowSeconds: Int,
    val acfMaxLagSeconds: Int,
    val acfHalfLifeSeconds: Float?,
    val inOutBias: Float,
    val initialCycleLength: Float,
    val initialCycleLengthRange: IntRange,
    val maxCycleLengthRange: IntRange,
    val targetCycleLengthSmoothingWindow: Int,
    val resonancePeakToleranceSeconds: Float,
    val resonanceMinPeakValue: Float,
    val windowLength: Int,
    val acfHistogramExpGain: Float,
    val acfHistogramSigmoidSteepness: Float,
    val acfHistogramSigmoidMidpoint: Float,
    val acfHistogramHalfLifeSeconds: Float?,
    val acfHistogramIgnoredLeadingLags: Int,
    val hrvPerPaceHalfLifeSamples: Float?,
) {
    init {
        require(acfMaxLagSeconds >= maxCycleLengthRange.last) {
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
                acfWindowSeconds = 24,
                acfMaxLagSeconds = 16,
                acfHalfLifeSeconds = 12f,
                inOutBias = 0f,
                initialCycleLength = 9f,
                initialCycleLengthRange = 4..16,
                maxCycleLengthRange = 4..16,
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
                // Per-pace HRV samples are slow and noisy; blend in each new one rather than
                // overwriting outright. Weight of an old reading halves every 3 further samples
                // at the same pace. Tune on-device.
                hrvPerPaceHalfLifeSamples = 3f,
            )
    }
}

fun BreathingConfig.defaultPattern() = BreathingPattern(inOutBias, initialCycleLength)
