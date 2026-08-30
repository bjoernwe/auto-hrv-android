package dev.upaya.autohrv.domain.spectral

/**
 * Tuning for the RR-interval spectrogram.
 *
 * [windowSeconds] is a power of two on the repository's 1 Hz grid so the FFT needs no
 * zero-padding; 128 s gives frequency resolution ~1/128 Hz ≈ 0.0078 Hz, resolving the Mayer-wave
 * / LF band (0.04-0.15 Hz) into roughly 14 bins. A new slice is computed from the trailing
 * [windowSeconds] of history every [hopSeconds] (heavy overlap, since hop << window), so slow
 * drift in the band shows up as a smoothly shifting pattern rather than jumps between disjoint
 * blocks. Only the most recent [maxSlices] are retained for display.
 */
data class SpectrogramConfig(
    val windowSeconds: Int,
    val hopSeconds: Int,
    val maxSlices: Int,
    val displayFreqRangeHz: ClosedFloatingPointRange<Float>,
    val mayerBandHz: ClosedFloatingPointRange<Float>,
) {
    init {
        require(windowSeconds > 0 && (windowSeconds and (windowSeconds - 1)) == 0) {
            "windowSeconds must be a power of two for the FFT"
        }
        require(hopSeconds > 0) { "hopSeconds must be positive" }
        require(maxSlices > 0) { "maxSlices must be positive" }
    }

    companion object {
        val DEFAULT =
            SpectrogramConfig(
                windowSeconds = 128,
                hopSeconds = 5,
                // 120 slices * 5s hop = 10 min of scrolling history.
                maxSlices = 120,
                // Mayer band with a little context on either side.
                displayFreqRangeHz = 0f..0.2f,
                mayerBandHz = 0.04f..0.15f,
            )
    }
}
