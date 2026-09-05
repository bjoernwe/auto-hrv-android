package dev.upaya.autohrv.ui.acf

import dev.upaya.autohrv.ui.commons.normalizeMinMax
import kotlin.math.exp

// Shaping of the accumulated ACF histogram: exp sharpens the dominant lags, the closing sigmoid
// keeps secondary peaks visible. Tune on-device.
private const val EXP_GAIN = 2.5f
private const val SIGMOID_STEEPNESS = 5f
private const val SIGMOID_MIDPOINT = 0.2f

// "Ignore up to lag 3": lags 1-3 are naturally high (short-lag autocorrelation) and would
// otherwise dominate the shaping normalization, squashing the real breathing-range peaks. Lag 0 is
// always ignored on top of this. They're capped into range rather than dropped, so they still show
// in the histogram.
private const val IGNORED_LEADING_LAGS = 3

/**
 * Shapes accumulated per-lag ACF sums into display heights in `[0, 1]`:
 * `cap → exp(EXP_GAIN · x) → normalize → sigmoid(SIGMOID_STEEPNESS · (x − SIGMOID_MIDPOINT))`.
 *
 * Lag 0 is always excluded — its correlation is fixed at 1.0 and carries no information — and its
 * output is always `0f`. [ignoredLeadingLags] reads as "ignore up to this lag": lags
 * `1..ignoredLeadingLags` are naturally high (short-lag autocorrelation) and are excluded from
 * *establishing* the normalization range, so they can't compress the real breathing-range peaks
 * down to noise. They are not dropped from the output though — they're capped to the `[0, 1]`
 * range established by the remaining lags (so they read as "at least as tall as the tallest real
 * peak" rather than disappearing) before continuing through the same exp/sigmoid shaping as every
 * other bin.
 *
 * The exponential sharpens the dominant lags; the closing sigmoid flattens the noise floor and
 * caps the winner so secondary and tertiary peaks stay visible instead of being swallowed by the
 * exponential's winner-takes-all behavior.
 */
internal fun shapeAcfHistogram(
    sums: List<Float>,
    ignoredLeadingLags: Int = IGNORED_LEADING_LAGS,
): List<Float> {
    if (sums.isEmpty()) return sums
    val lagsBeyondZero = sums.drop(1)
    if (lagsBeyondZero.isEmpty()) return List(sums.size) { 0f }

    // Leave at least one lag to establish the range even if ignoredLeadingLags covers everything.
    val effectiveIgnored = ignoredLeadingLags.coerceIn(0, lagsBeyondZero.size - 1)
    val normRange = lagsBeyondZero.drop(effectiveIgnored)
    val capped = lagsBeyondZero.normalizeMinMax(normRange.min(), normRange.max())

    val shaped =
        capped
            .map { exp(EXP_GAIN * it) }
            .normalizeMinMax()
            .map { sigmoid(SIGMOID_STEEPNESS * (it - SIGMOID_MIDPOINT)) }
    return listOf(0f) + shaped
}

private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))
