package com.polar.polarsdkecghrdemo.domain.hr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.sqrt

class HeartRateStatsUseCase @Inject constructor() {

    operator fun invoke(hrHistory: Flow<List<Int>>): Flow<Float?> =
        hrHistory.map { computeSmoothness(it) }

    private fun computeSmoothness(history: List<Int>): Float? {
        if (history.size < 2) return null
        val floats = history.map { it.toFloat() }
        val mean = floats.average().toFloat()
        val std = sqrt(floats.map { d -> (d - mean) * (d - mean) }.average().toFloat())
        if (std < 1e-6f) return 0f
        val normalized = floats.map { (it - mean) / std }
        val meanSquaredDiff = normalized
            .zipWithNext { a, b -> (b - a) * (b - a) }
            .average()
            .toFloat()
        return 2f - sqrt(meanSquaredDiff)
    }
}
