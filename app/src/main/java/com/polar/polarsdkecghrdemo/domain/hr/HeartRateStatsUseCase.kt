package com.polar.polarsdkecghrdemo.domain.hr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.collections.average
import kotlin.collections.map
import kotlin.math.sqrt

class HeartRateStatsUseCase @Inject constructor() {

    operator fun invoke(hrHistory: Flow<List<Int>>): Flow<Float?> =
        hrHistory.map { computeSmoothness(it) }

    private fun normalize(values: List<Int>): List<Float> {
        val floats = values.map { it.toFloat() }
        val mean = floats.average().toFloat()
        val std = sqrt(floats.map { d -> (d - mean) * (d - mean) }.average().toFloat())
        if (std < 1e-6f) return values.map { 0f }
        val normalized = floats.map { (it - mean) / std }
        return normalized
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
}
