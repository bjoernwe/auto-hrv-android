package dev.upaya.autohrv.domain.breathing

import dev.upaya.autohrv.domain.normalizeMinMax
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import kotlin.math.exp

/**
 * Element-wise running sum of successive ACF emissions. A size mismatch (including an empty
 * accumulator on the first emission) restarts the sum from [acf], so the accumulator always
 * tracks the current ACF length.
 */
internal fun accumulateAcf(
    acc: List<Float>,
    acf: List<Float>,
): List<Float> {
    if (acc.size != acf.size) return acf
    return acc.zip(acf) { a, b -> a + b }
}

/**
 * Shapes accumulated per-lag ACF sums into display heights in `[0, 1]`:
 * `normalize → exp(expGain · x) → normalize → sigmoid(sigmoidSteepness · (x − sigmoidMidpoint))`.
 *
 * The exponential sharpens the dominant lags; the closing sigmoid flattens the noise floor and
 * caps the winner so secondary and tertiary peaks stay visible instead of being swallowed by the
 * exponential's winner-takes-all behavior.
 */
internal fun shapeAcfHistogram(
    sums: List<Float>,
    expGain: Float,
    sigmoidSteepness: Float,
    sigmoidMidpoint: Float,
): List<Float> {
    if (sums.isEmpty()) return sums
    return sums
        .normalizeMinMax()
        .map { exp(expGain * it) }
        .normalizeMinMax()
        .map { sigmoid(sigmoidSteepness * (it - sigmoidMidpoint)) }
}

private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

/**
 * Accumulates every emitted ACF element-wise over the lifetime of the flow and shapes the running
 * sums into histogram heights. `null` values (no ACF yet) are ignored, and consecutive identical
 * ACFs are deduplicated so slider-driven recomputations of the same window are not double-counted.
 */
internal fun Flow<List<Float>?>.accumulatedAcfHistogram(
    expGain: Float,
    sigmoidSteepness: Float,
    sigmoidMidpoint: Float,
): Flow<List<Float>> =
    mapNotNull { it }
        .distinctUntilChanged()
        .scan(emptyList<Float>()) { acc, acf -> accumulateAcf(acc, acf) }
        .map { shapeAcfHistogram(it, expGain, sigmoidSteepness, sigmoidMidpoint) }
