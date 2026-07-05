package dev.upaya.autohrv.domain.breathing

import dev.upaya.autohrv.domain.normalizeMinMax
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import kotlin.math.exp
import kotlin.math.pow

/**
 * Element-wise running sum of successive ACF emissions, decaying the prior accumulator by
 * [decay] before adding the new value (`decay = 1f` is a plain running sum). A size mismatch
 * (including an empty accumulator on the first emission) restarts the sum from [acf], so the
 * accumulator always tracks the current ACF length.
 */
internal fun accumulateAcf(
    acc: List<Float>,
    acf: List<Float>,
    decay: Float,
): List<Float> {
    if (acc.size != acf.size) return acf
    return acc.zip(acf) { a, b -> a * decay + b }
}

/**
 * Shapes accumulated per-lag ACF sums into display heights in `[0, 1]`:
 * `cap → exp(expGain · x) → normalize → sigmoid(sigmoidSteepness · (x − sigmoidMidpoint))`.
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
    config: BreathingConfig,
): List<Float> {
    if (sums.isEmpty()) return sums
    val lagsBeyondZero = sums.drop(1)
    if (lagsBeyondZero.isEmpty()) return List(sums.size) { 0f }

    // Leave at least one lag to establish the range even if ignoredLeadingLags covers everything.
    val effectiveIgnored = config.acfHistogramIgnoredLeadingLags.coerceIn(0, lagsBeyondZero.size - 1)
    val normRange = lagsBeyondZero.drop(effectiveIgnored)
    val capped = lagsBeyondZero.normalizeMinMax(normRange.min(), normRange.max())

    val shaped =
        capped
            .map { exp(config.acfHistogramExpGain * it) }
            .normalizeMinMax()
            .map { sigmoid(config.acfHistogramSigmoidSteepness * (it - config.acfHistogramSigmoidMidpoint)) }
    return listOf(0f) + shaped
}

private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

/**
 * Accumulates every emitted ACF element-wise over the lifetime of the flow and shapes the running
 * sums into histogram heights. `null` values (no ACF yet) are ignored, and consecutive identical
 * ACFs are deduplicated so slider-driven recomputations of the same window are not double-counted.
 *
 * [BreathingConfig.acfHistogramHalfLifeSeconds], if non-null, decays older emissions so the
 * histogram tracks the recent session rather than accumulating indefinitely. Emissions arrive
 * roughly once per second (after dedup), so the half-life in seconds roughly matches the
 * half-life in emissions.
 */
internal fun Flow<List<Float>?>.accumulatedAcfHistogram(config: BreathingConfig): Flow<List<Float>> {
    val halfLifeSeconds = config.acfHistogramHalfLifeSeconds
    val decay = if (halfLifeSeconds == null) 1f else 0.5f.pow(1f / halfLifeSeconds)
    return mapNotNull { it }
        .distinctUntilChanged()
        .scan(emptyList<Float>()) { acc, acf -> accumulateAcf(acc, acf, decay) }
        .map { shapeAcfHistogram(it, config) }
}
