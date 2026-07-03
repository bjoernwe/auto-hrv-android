package dev.upaya.autohrv.domain.breathing

import kotlin.math.pow
import kotlin.math.sqrt

/** Minimum number of overlapping sample pairs required to estimate a lag. */
private const val MIN_OVERLAP = 8

/** Variance below this is treated as a constant (degenerate) signal. */
private const val VARIANCE_EPSILON = 1e-12

/**
 * Direct time-domain autocorrelation of a uniformly sampled series, computed as a per-lag
 * weighted Pearson correlation.
 *
 * For each lag `k` in `0..maxLag` the overlapping pairs `(values[t], values[t + k])` are
 * correlated. Each of the two columns is re-centered on its own weighted mean and scaled by its
 * own weighted standard deviation, so by the Cauchy–Schwarz inequality the result is bounded in
 * `[-1, 1]` for **any** weighting, with `r[0] == 1` exactly. This makes the estimate robust to
 * slow trends (each lag re-centers itself) and keeps the resonance threshold on the values
 * meaningful. `values` is assumed to be on a 1 Hz grid, so `maxLag` is in seconds and the index
 * of the returned list equals the lag in seconds.
 *
 * Recency weighting: sample `i` gets weight `0.5^((n - 1 - i) / halfLifeSeconds)` — the newest
 * sample has weight 1 and each `halfLifeSeconds` step into the past halves the weight. A `null`
 * half-life gives uniform weights. Because Pearson correlation is invariant to a per-lag constant
 * scaling of the weights, it does not matter whether the pair weight uses the older or the newer
 * member; we use the newer member (`w[t + k]`).
 *
 * @return a list of size `maxLag + 1`, or `null` when there is too little data
 *   (`n < maxLag + MIN_OVERLAP`) or the signal is effectively constant.
 */
internal fun weightedAutoCorrelation(
    values: List<Float>,
    maxLag: Int,
    halfLifeSeconds: Float? = null,
): List<Float>? {
    val n = values.size
    if (maxLag < 0 || n < maxLag + MIN_OVERLAP) return null

    val x = DoubleArray(n) { values[it].toDouble() }
    val w = DoubleArray(n) { i ->
        if (halfLifeSeconds == null) 1.0
        else 0.5.pow((n - 1 - i).toDouble() / halfLifeSeconds.toDouble())
    }

    // Reject a constant signal up front using the full-window weighted variance.
    if (weightedVariance(x, w, 0, n) < VARIANCE_EPSILON) return null

    return (0..maxLag).map { k -> pearsonAtLag(x, w, k) }
}

/** Weighted Pearson correlation of `(x[t], x[t + k])` over the overlapping range. */
private fun pearsonAtLag(x: DoubleArray, w: DoubleArray, k: Int): Float {
    val n = x.size
    val m = n - k // number of overlapping pairs

    var sumW = 0.0
    var sumX = 0.0
    var sumY = 0.0
    for (t in 0 until m) {
        val v = w[t + k]
        sumW += v
        sumX += v * x[t]
        sumY += v * x[t + k]
    }
    if (sumW <= 0.0) return 0f

    val meanX = sumX / sumW
    val meanY = sumY / sumW

    var sxy = 0.0
    var sxx = 0.0
    var syy = 0.0
    for (t in 0 until m) {
        val v = w[t + k]
        val dx = x[t] - meanX
        val dy = x[t + k] - meanY
        sxy += v * dx * dy
        sxx += v * dx * dx
        syy += v * dy * dy
    }

    val denom = sxx * syy
    if (denom < VARIANCE_EPSILON) return 0f
    return (sxy / sqrt(denom)).toFloat()
}

/** Weighted variance of `x` over `[from, to)`. */
private fun weightedVariance(x: DoubleArray, w: DoubleArray, from: Int, to: Int): Double {
    var sumW = 0.0
    var sumX = 0.0
    for (t in from until to) {
        sumW += w[t]
        sumX += w[t] * x[t]
    }
    if (sumW <= 0.0) return 0.0
    val mean = sumX / sumW
    var s = 0.0
    for (t in from until to) {
        val d = x[t] - mean
        s += w[t] * d * d
    }
    return s / sumW
}
