package dev.upaya.autohrv.domain

/** Values whose spread is below this are treated as constant (degenerate). */
private const val RANGE_EPSILON = 1e-12f

/**
 * Min-max normalizes the values into `[0, 1]`: the minimum maps to 0 and the maximum to 1.
 *
 * An empty list returns empty. A constant (all-equal) list carries no contrast information, so
 * every element maps to the neutral midpoint `0.5`. Negative inputs are handled naturally.
 */
internal fun List<Float>.normalizeMinMax(): List<Float> {
    if (isEmpty()) return this
    return normalizeMinMax(min(), max())
}

/**
 * Min-max normalizes the values against an externally supplied `[min, max]` range instead of the
 * list's own extremes, clamping the result to `[0, 1]`. Useful when some values (e.g. lags
 * excluded from establishing the range) may fall outside `[min, max]` and should simply cap at
 * the nearest bound rather than skewing the scale.
 *
 * An empty list returns empty. A degenerate range carries no contrast information, so every
 * element maps to the neutral midpoint `0.5`.
 */
internal fun List<Float>.normalizeMinMax(
    min: Float,
    max: Float,
): List<Float> {
    if (isEmpty()) return this
    val range = max - min
    if (range < RANGE_EPSILON) return List(size) { 0.5f }
    return map { ((it - min) / range).coerceIn(0f, 1f) }
}
