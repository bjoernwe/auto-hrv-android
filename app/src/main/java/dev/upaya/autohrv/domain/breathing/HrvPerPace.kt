package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan
import kotlin.math.pow
import kotlin.math.roundToInt

/** A single HRV (RMSSD) measurement recorded while breathing at [paceSeconds]. */
internal data class HrvPaceSample(
    val paceSeconds: Float,
    val hrv: Float,
)

/**
 * The three values [hrvPaceSampleOrNull] needs, pulled from a stats snapshot and the current
 * breathing [pattern]: the paced target, the resonant lag from the ACF, and the HRV magnitude.
 * `null` unless [stats] carries both an ACF peak and an RMSSD.
 */
internal fun hrvPaceInputsOrNull(
    stats: TimeSeriesStats?,
    pattern: BreathingPattern,
): Triple<Float, Float, Float>? {
    val acfPeak = stats?.resampledRrsStats?.autoCorrelationPeak ?: return null
    val rmssd = stats.beatRrsStats?.rmssd ?: return null
    return Triple(pattern.cycleLengthSeconds, acfPeak, rmssd)
}

/**
 * A per-pace HRV sample recording [rmssd] at [paceSeconds], or `null` when it shouldn't be
 * recorded: the paced target and the measured ACF peak ([acfPeak]) must round to the same lag,
 * otherwise the pace isn't actually the one the HRV was measured at.
 */
internal fun hrvPaceSampleOrNull(
    paceSeconds: Float,
    acfPeak: Float,
    rmssd: Float,
): HrvPaceSample? {
    if (paceSeconds.roundToInt() != acfPeak.roundToInt()) return null
    return HrvPaceSample(paceSeconds, rmssd)
}

/**
 * Records [hrv] at the bar for [paceSeconds] (rounded to the nearest integer, clamped into the
 * accumulator's bounds), leaving every other bar untouched. The accumulator [acc] both carries the
 * running values and defines the bar count; [accumulatedHrvPerPace] seeds it with the right size.
 *
 * A bar is `null` until it receives its first sample, which replaces the null outright. A later
 * sample at the same pace is blended into the existing value instead, with [decay] the weight kept
 * on the prior value — `0f` (the default) is a plain overwrite, and values closer to `1f` retain
 * more history, smoothing out noisy single measurements. This mirrors the half-life decay
 * [accumulateAcf] applies to the ACF histogram.
 */
internal fun updateHrvPerPace(
    acc: List<Float?>,
    paceSeconds: Float,
    hrv: Float,
    decay: Float = 0f,
): List<Float?> {
    val index = paceSeconds.roundToInt().coerceIn(0, acc.size - 1)
    return acc.mapIndexed { i, v ->
        when {
            i != index -> v
            v == null -> hrv
            else -> v * decay + hrv * (1f - decay)
        }
    }
}

/**
 * Accumulates HRV at each integer breathing pace over the lifetime of the flow, producing bars
 * sized [size]. Each bar holds the last-known (optionally smoothed) RMSSD recorded at that pace, or
 * `null` for a pace never sampled. The values stay in raw RMSSD units; scaling them to bar heights
 * for display is a UI concern (see the view model).
 *
 * A later sample at the same pace is blended into the bar with a half-life of [sampleHalfLife]
 * *samples* — after that many further samples at the same pace, the weight of the original value
 * has fallen to half — smoothing out noisy individual RMSSD readings. `null` disables smoothing
 * and each sample overwrites the previous one. The half-life is counted in samples rather than
 * wall-clock time (unlike e.g. [BreathingConfig.acfHistogramHalfLifeSeconds]) because samples for
 * a given pace can be slow and irregular to arrive; decaying by elapsed time would fade a bar out
 * while still waiting for its next reading.
 */
internal fun Flow<HrvPaceSample>.accumulatedHrvPerPace(
    size: Int,
    sampleHalfLife: Float? = null,
): Flow<List<Float?>> {
    val decay = if (sampleHalfLife == null) 0f else 0.5f.pow(1f / sampleHalfLife)
    return scan(List<Float?>(size) { null }) { acc, sample -> updateHrvPerPace(acc, sample.paceSeconds, sample.hrv, decay) }
}
