package dev.upaya.autohrv.domain.spectral

/**
 * Tuning for one band of the RR-interval spectrogram.
 *
 * [windowSeconds] is a power of two on the repository's 1 Hz grid so the FFT needs no zero-padding,
 * and is chosen long enough to faithfully resolve the band's slowest frequency (its slowest
 * resolvable oscillation is one full cycle per window, at `1/windowSeconds` Hz). A shorter window
 * fills — and so starts updating — sooner, which is why faster bands can use a smaller one. A new
 * slice is computed from the trailing [windowSeconds] of history every [hopSeconds] (heavy overlap,
 * since hop << window), so drift within the band shows as a smoothly shifting pattern rather than
 * jumps between disjoint blocks. Only the most recent [maxSlices] are retained for display.
 */
data class SpectrogramBand(
    val label: String,
    val windowSeconds: Int,
    val hopSeconds: Int,
    val maxSlices: Int,
    val freqRangeHz: ClosedFloatingPointRange<Float>,
) {
    init {
        require(windowSeconds > 0 && (windowSeconds and (windowSeconds - 1)) == 0) {
            "windowSeconds must be a power of two for the FFT"
        }
        require(hopSeconds > 0) { "hopSeconds must be positive" }
        require(maxSlices > 0) { "maxSlices must be positive" }
    }
}

/**
 * The set of frequency bands the spectrogram computes independently over the same RR signal. Each
 * band has its own window, hop and color normalization (the latter a display concern, applied in
 * the chart), so faster oscillations update sooner and stay visible instead of being drowned out by
 * the dominant low-frequency power.
 */
data class SpectrogramConfig(
    val bands: List<SpectrogramBand>,
) {
    init {
        require(bands.isNotEmpty()) { "at least one band is required" }
    }

    companion object {
        val DEFAULT =
            SpectrogramConfig(
                bands =
                    listOf(
                        SpectrogramBand(
                            label = "BREATH",
                            windowSeconds = 64,
                            hopSeconds = 1,
                            maxSlices = 120,
                            freqRangeHz = 0f..0.25f,
                        ),
                    ),
            )
    }
}
