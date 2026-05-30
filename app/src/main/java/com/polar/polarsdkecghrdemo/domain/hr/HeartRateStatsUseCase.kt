package com.polar.polarsdkecghrdemo.domain.hr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import javax.inject.Inject
import kotlin.math.sqrt

class HeartRateStatsUseCase @Inject constructor() {

    fun smoothness(hrHistory: Flow<List<Int>>): Flow<Float?> =
        hrHistory.map { computeSmoothness(it) }

    fun powerSpectrum(hrHistory: Flow<List<Int>>): Flow<List<Float>?> =
        hrHistory.map { computePowerSpectrum(it) }

    private fun normalize(values: List<Int>): List<Float> {
        val floats = values.map { it.toFloat() }
        val mean = floats.average().toFloat()
        val std = sqrt(floats.map { d -> (d - mean) * (d - mean) }.average().toFloat())
        if (std < 1e-6f) return values.map { 0f }
        return floats.map { (it - mean) / std }
    }

    private fun computeSmoothness(values: List<Int>): Float? {
        if (values.size < 2) return null
        val normalized = normalize(values)
        val meanSquaredDiff = normalized
            .zipWithNext { a, b -> (b - a) * (b - a) }
            .average()
            .toFloat()
        return 2f - sqrt(meanSquaredDiff)
    }

    private fun computePowerSpectrum(values: List<Int>): List<Float>? {
        if (values.size < 4) return null
        val n = nextPowerOf2(values.size)
        val mean = values.average()
        // Zero-pad to next power of 2 and subtract mean to suppress DC
        val input = DoubleArray(n) { i ->
            if (i < values.size) values[i].toDouble() - mean else 0.0
        }
        val result = FastFourierTransformer(DftNormalization.STANDARD)
            .transform(input, TransformType.FORWARD)
        // One-sided spectrum: skip DC (bin 0), return bins 1..n/2
        return (1..n / 2).map { i ->
            val re = result[i].real
            val im = result[i].imaginary
            (re * re + im * im).toFloat()
        }
    }

    private fun nextPowerOf2(n: Int): Int {
        var p = 1
        while (p < n) p = p shl 1
        return p
    }
}