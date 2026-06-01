package com.polar.polarsdkecghrdemo.domain.experiment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

data class TimeSeriesStats(
    val autoCorrelation: List<Float>?,
    val autoCorrelationPeak: Float?,
    val peakPower: Float?,
    val periodicity: Float?,
    val powerSpectrum: List<Float>?,
    val fallingToRaisingRatio: Float?,
    val smoothness: Float?,
    val stdDev: Float?,
)

private class SpectrumData(val fullSpectrum: DoubleArray) {
    val oneSided: List<Float> get() = (1..fullSpectrum.size / 2).map { i -> fullSpectrum[i].toFloat() }
}

internal class TimeSeriesStatsUseCase @Inject constructor() {

    operator fun invoke(ts: Flow<List<Int>>): Flow<TimeSeriesStats> {
        return ts.map { ts ->
            val spectrumData = computeSpectrumData(ts)
            val acf = spectrumData?.let { computeAutoCorrelation(it.fullSpectrum) }
            TimeSeriesStats(
                autoCorrelation = acf,
                autoCorrelationPeak = acf?.let { findBreathingCycleLength(it) },
                peakPower = spectrumData?.oneSided?.drop(1)?.max(),
                periodicity = computePeriodicity(spectrumData?.oneSided),
                powerSpectrum = spectrumData?.oneSided,
                fallingToRaisingRatio = computeFallingToRaisingRatio(ts),
                smoothness = computeSmoothness(ts),
                stdDev = computeStdDev(ts),
            )
        }
    }

    // Searches for the highest ACF peak in the lag range corresponding to 3–15 s breathing cycles.
    // Assumes 1 s per sample, so lag == cycle length in seconds directly.
    private fun findBreathingCycleLength(acf: List<Float>): Float? {
        val minLag = 6
        val maxLag = 14.coerceAtMost(acf.size - 1)
        if (minLag > maxLag) return null
        val peakLag = (minLag..maxLag).maxByOrNull { acf[it] } ?: return null
        return peakLag.toFloat()
    }

    private fun normalize(values: List<Int>): List<Float> {
        val floats = values.map { it.toFloat() }
        val mean = floats.average().toFloat()
        val std = sqrt(floats.map { d -> (d - mean) * (d - mean) }.average().toFloat())
        if (std < 1e-6f) return values.map { 0f }
        return floats.map { (it - mean) / std }
    }

    private fun computeSmoothness(ts: List<Int>): Float? {
        if (ts.size < 2) return null
        val normalized = normalize(ts)
        val meanSquaredDiff = normalized
            .zipWithNext { a, b -> (b - a) * (b - a) }
            .average()
            .toFloat()
        return 2f - sqrt(meanSquaredDiff)
    }

    private fun computeSpectrumData(ts: List<Int>): SpectrumData? {
        if (ts.size < 4) return null
        val n = nextPowerOf2(ts.size)
        val mean = ts.average()
        val len = ts.size
        // Apply a Hann/Hamming window to the data, zero-pad to next power of 2, and subtract mean to suppress DC
        val input = DoubleArray(n) { i ->
            if (i < len) {
                //val w = 0.5 * (1.0 - cos(2.0 * PI * i / (len - 1)))
                val w = 0.54 - 0.46 * cos(2.0 * PI * i / (len - 1))
                (ts[i].toDouble() - mean) * w
            } else {
                0.0
            }
        }
        val result = FastFourierTransformer(DftNormalization.STANDARD)
            .transform(input, TransformType.FORWARD)
        val fullPower = DoubleArray(n) { i ->
            val re = result[i].real
            val im = result[i].imaginary
            re * re + im * im
        }
        // One-sided spectrum: skip DC (bin 0), return bins 1..n/2
        return SpectrumData(fullPower)
    }

    // Wiener-Khinchin: autocorrelation = IFFT(power spectrum), normalized to lag-0 = 1
    private fun computeAutoCorrelation(fullPower: DoubleArray): List<Float> {
        val acfComplex = FastFourierTransformer(DftNormalization.STANDARD)
            .transform(fullPower, TransformType.INVERSE)
        val zero = acfComplex[0].real.coerceAtLeast(1e-10)
        val halfN = fullPower.size / 2
        return (0 until halfN).map { i -> (acfComplex[i].real / zero).toFloat() }
    }

    private fun computePeriodicity(spectrum: List<Float>?): Float? {
        if (spectrum === null) return null
        if (spectrum.size < 3) return null
        // Drop the first bin — it reflects slow HR trends rather than rhythmic periodicity
        val bins = spectrum.drop(1)
        val total = bins.sum()
        if (total < 1e-10f) return null
        val entropy = bins.fold(0.0) { acc, p ->
            val prob = (p / total).toDouble()
            if (prob > 0.0) acc - prob * ln(prob) else acc
        }.toFloat()
        val maxEntropy = ln(bins.size.toDouble()).toFloat()
        return if (maxEntropy > 0f) 1f - (entropy / maxEntropy) else 0f
    }

    private fun computeFallingToRaisingRatio(ts: List<Int>): Float? {
        if (ts.size < 2) return null
        var raising = 0
        var falling = 0
        ts.zipWithNext { a, b ->
            when {
                b > a -> raising++
                b < a -> falling++
            }
        }
        if (raising == 0) return null
        return falling.toFloat() / raising.toFloat()
    }

    private fun computeStdDev(ts: List<Int>): Float? {
        if (ts.size < 2) return null
        val mean = ts.average().toFloat()
        return sqrt(ts.map { (it - mean) * (it - mean) }.average().toFloat())
    }

    private fun nextPowerOf2(n: Int): Int {
        var p = 1
        while (p < n) p = p shl 1
        return p
    }
}
