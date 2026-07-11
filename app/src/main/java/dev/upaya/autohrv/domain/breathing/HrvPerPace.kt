package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlin.math.roundToInt

/** A single HRV (RMSSD) measurement recorded while breathing at [paceSeconds]. */
internal data class HrvPaceSample(
    val paceSeconds: Float,
    val hrv: Float,
)

/**
 * Records [hrv] at the bar for [paceSeconds] (rounded to the nearest integer, clamped into
 * `0 until size`), leaving every other bar untouched. A size mismatch (including an empty
 * accumulator on the first sample) restarts from `size` ones, so the accumulator always tracks
 * the current bar count.
 */
internal fun updateHrvPerPace(
    acc: List<Float>,
    size: Int,
    paceSeconds: Float,
    hrv: Float,
): List<Float> {
    val base = if (acc.size == size) acc else List(size) { 1f }
    val index = paceSeconds.roundToInt().coerceIn(0, size - 1)
    return base.mapIndexed { i, v -> if (i == index) hrv else v }
}

/** Scales bars to `[0, 1]` relative to the largest recorded value. All-zero input stays all-zero. */
internal fun normalizeHrvPerPace(raw: List<Float>): List<Float> {
    val max = raw.maxOrNull() ?: return raw
    if (max <= 0f) return raw
    return raw.map { it / max }
}

/**
 * Accumulates the last-known HRV at each integer breathing pace over the lifetime of the flow,
 * producing bars sized [size] and normalized to `[0, 1]` relative to the largest recorded value.
 * A later sample at the same pace overwrites the earlier one; paces never sampled stay at the
 * unscaled baseline of `1`.
 */
internal fun Flow<HrvPaceSample>.accumulatedHrvPerPace(size: Int): Flow<List<Float>> =
    scan(List(size) { 1f }) { acc, sample -> updateHrvPerPace(acc, size, sample.paceSeconds, sample.hrv) }
        .map { normalizeHrvPerPace(it) }
