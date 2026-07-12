package dev.upaya.autohrv.ui.hr

/** Height an unsampled bar (`null`) normalizes to, relative to the largest recorded value. */
private const val UNSAMPLED_BASELINE = 1f

/**
 * Turns raw per-pace HRV (RMSSD) values into bar heights in `[0, 1]`, scaled relative to the
 * largest recorded value. Never-sampled bars (`null`) are first replaced with the
 * [UNSAMPLED_BASELINE]; with no samples yet every bar sits at the baseline.
 */
internal fun normalizeHrvPerPace(raw: List<Float?>): List<Float> {
    val filled = raw.map { it ?: UNSAMPLED_BASELINE }
    val max = filled.maxOrNull() ?: return filled
    if (max <= 0f) return filled
    return filled.map { it / max }
}