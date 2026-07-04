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
 * `normalize → exp(expGain · x) → normalize → sigmoid(sigmoidSteepness · (x − sigmoidMidpoint))`.
 *
 * The leading [ignoredLeadingLags] bins (e.g. lag 0's fixed 1.0 and the naturally-high lag 1/2
 * autocorrelation) are excluded from the shaping pipeline and forced to `0f`, so they can't set
 * the normalization ceiling and squash the real breathing-range peaks.
 *
 * The exponential sharpens the dominant lags; the closing sigmoid flattens the noise floor and
 * caps the winner so secondary and tertiary peaks stay visible instead of being swallowed by the
 * exponential's winner-takes-all behavior.
 */
internal fun shapeAcfHistogram(
    sums: List<Float>,
    ignoredLeadingLags: Int,
    expGain: Float,
    sigmoidSteepness: Float,
    sigmoidMidpoint: Float,
): List<Float> {
    if (sums.isEmpty()) return sums
    if (ignoredLeadingLags >= sums.size) return List(sums.size) { 0f }
    val shaped =
        sums
            .drop(ignoredLeadingLags)
            .normalizeMinMax()
            .map { exp(expGain * it) }
            .normalizeMinMax()
            .map { sigmoid(sigmoidSteepness * (it - sigmoidMidpoint)) }
    return List(ignoredLeadingLags) { 0f } + shaped
}

private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

/**
 * Accumulates every emitted ACF element-wise over the lifetime of the flow and shapes the running
 * sums into histogram heights. `null` values (no ACF yet) are ignored, and consecutive identical
 * ACFs are deduplicated so slider-driven recomputations of the same window are not double-counted.
 *
 * [halfLifeSeconds], if non-null, decays older emissions so the histogram tracks the recent
 * session rather than accumulating indefinitely. Emissions arrive roughly once per second (after
 * dedup), so the half-life in seconds roughly matches the half-life in emissions.
 */
internal fun Flow<List<Float>?>.accumulatedAcfHistogram(
    halfLifeSeconds: Float?,
    ignoredLeadingLags: Int,
    expGain: Float,
    sigmoidSteepness: Float,
    sigmoidMidpoint: Float,
): Flow<List<Float>> {
    val decay = if (halfLifeSeconds == null) 1f else 0.5f.pow(1f / halfLifeSeconds)
    return mapNotNull { it }
        .distinctUntilChanged()
        .scan(emptyList<Float>()) { acc, acf -> accumulateAcf(acc, acf, decay) }
        .map { shapeAcfHistogram(it, ignoredLeadingLags, expGain, sigmoidSteepness, sigmoidMidpoint) }
}
