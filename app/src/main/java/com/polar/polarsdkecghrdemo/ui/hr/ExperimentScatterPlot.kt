package com.polar.polarsdkecghrdemo.ui.hr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord

private val DotColor = Color(0xFFC00000)

@Composable
fun ExperimentScatterPlot(history: List<ExperimentRecord>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        if (history.isEmpty()) return@Canvas

        val padding = 16.dp.toPx()
        val plotW = size.width - 2 * padding
        val plotH = size.height - 2 * padding
        val dotRadius = 10.dp.toPx()

        val ratios = history.map { it.params.outToInRatio }
        val lengths = history.map { it.params.cycleLengthSeconds }
        val xMin = ratios.min(); val xMax = ratios.max()
        val yMin = lengths.min(); val yMax = lengths.max()
        val xRange = (xMax - xMin).coerceAtLeast(0.1f)
        val yRange = (yMax - yMin).coerceAtLeast(0.1f)

        history.forEach { record ->
            val cx = padding + (record.params.outToInRatio - xMin) / xRange * plotW
            // Y is inverted: higher cycle length → top of canvas
            val cy = size.height - padding - (record.params.cycleLengthSeconds - yMin) / yRange * plotH
            drawCircle(
                color = DotColor.copy(alpha = record.periodicity.coerceIn(0f, 1f)),
                radius = dotRadius,
                center = Offset(cx, cy),
            )
        }
    }
}
