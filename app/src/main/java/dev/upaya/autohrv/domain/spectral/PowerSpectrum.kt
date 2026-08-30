package dev.upaya.autohrv.domain.spectral

import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos

private val fft = FastFourierTransformer(DftNormalization.STANDARD)

/** Hann window of the given size, values in `[0, 1]`. */
internal fun hannWindow(size: Int): DoubleArray = DoubleArray(size) { i -> 0.5 * (1 - cos(2 * PI * i / (size - 1))) }

// powerSpectrum() is called once per hop on a window size that never changes at runtime, so the
// Hann window and its energy (both a function of size alone) are cached rather than rebuilt every
// call. Since multiple bands can be active concurrently, the cache must be thread-safe and
// accommodate multiple sizes.
private val windowCache = ConcurrentHashMap<Int, Pair<DoubleArray, Double>>()

private fun hannWindowAndEnergy(size: Int): Pair<DoubleArray, Double> {
    return windowCache.computeIfAbsent(size) { s ->
        val window = hannWindow(s)
        window to window.sumOf { it * it }
    }
}

/**
 * One-sided power spectrum of [samples] (uniformly sampled at [sampleRateHz]). The mean is
 * removed and a Hann window applied before transforming, so a slowly drifting RR baseline
 * doesn't spill into the low-frequency bins as spectral leakage. [samples].size must be a power
 * of two.
 *
 * @return power per bin for bins `0..size/2` (DC to Nyquist); index `k` corresponds to the
 *   frequency returned by [frequencyBinsHz] at the same index. Normalized by the window's own
 *   energy so power stays comparable across slices regardless of the window shape.
 */
internal fun powerSpectrum(
    samples: List<Int>,
): List<Float> {
    val n = samples.size
    val (window, windowEnergy) = hannWindowAndEnergy(n)
    val mean = samples.average()
    val windowed = DoubleArray(n) { i -> (samples[i] - mean) * window[i] }
    val spectrum = fft.transform(windowed, TransformType.FORWARD)
    return (0..n / 2).map { k ->
        val c = spectrum[k]
        ((c.real * c.real + c.imaginary * c.imaginary) / windowEnergy).toFloat()
    }
}

/** Frequency (Hz) at each bin of a spectrum computed from [windowSize] samples at [sampleRateHz]. */
internal fun frequencyBinsHz(
    windowSize: Int,
    sampleRateHz: Double,
): List<Float> = (0..windowSize / 2).map { k -> (k * sampleRateHz / windowSize).toFloat() }

/**
 * Indices into [frequencyBinsHz] whose frequency falls within [range]. Bin 0 (DC — the mean
 * removed before transforming, not an oscillation) is always excluded, even when [range] starts
 * at 0: the slowest bin an FFT can actually resolve is bin 1, at `1/windowSize` Hz, one full
 * cycle per window.
 */
internal fun frequencyBinIndicesIn(
    windowSize: Int,
    sampleRateHz: Double,
    range: ClosedFloatingPointRange<Float>,
): List<Int> = indicesIn(frequencyBinsHz(windowSize, sampleRateHz), range)

/** The frequencies (Hz) selected by [frequencyBinIndicesIn] for the same arguments. */
internal fun frequencyBinsHzIn(
    windowSize: Int,
    sampleRateHz: Double,
    range: ClosedFloatingPointRange<Float>,
): List<Float> {
    val bins = frequencyBinsHz(windowSize, sampleRateHz)
    return indicesIn(bins, range).map { bins[it] }
}

private fun indicesIn(
    bins: List<Float>,
    range: ClosedFloatingPointRange<Float>,
): List<Int> = (1..bins.lastIndex).filter { bins[it] in range }
