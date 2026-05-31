package com.polar.polarsdkecghrdemo.ui.hr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord

private val DotColor = Color(0xFFC00000)
private val MeanColor = Color(0xFF1565C0)

@Composable
fun ExperimentScatterPlot(
    history: List<ExperimentRecord>,
    samplingMean: BreathingPattern? = null,
    modifier: Modifier = Modifier,
) {
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

        // Include the sampling mean in the axis range so the marker is always in frame.
        val lengths = history.map { it.params.cycleLengthSeconds } + listOfNotNull(samplingMean?.cycleLengthSeconds)
        val ratios = history.map { it.params.outToInRatio } + listOfNotNull(samplingMean?.outToInRatio)
        val xMin = lengths.min(); val xMax = lengths.max()
        val yMin = ratios.min(); val yMax = ratios.max()
        val xRange = (xMax - xMin).coerceAtLeast(0.1f)
        val yRange = (yMax - yMin).coerceAtLeast(0.1f)

        fun offsetOf(length: Float, ratio: Float): Offset {
            val cx = padding + (length - xMin) / xRange * plotW
            // Y is inverted: higher out:in ratio → top of canvas
            val cy = size.height - padding - (ratio - yMin) / yRange * plotH
            return Offset(cx, cy)
        }

        history.forEach { record ->
            drawCircle(
                color = DotColor.copy(alpha = record.periodicity.coerceIn(0f, 1f)),
                radius = dotRadius,
                center = offsetOf(record.params.cycleLengthSeconds, record.params.outToInRatio),
            )
        }

        // Draw the current sampling mean as a hollow ring with a crosshair.
        samplingMean?.let { mean ->
            val center = offsetOf(mean.cycleLengthSeconds, mean.outToInRatio)
            val strokeWidth = 2.dp.toPx()
            drawCircle(
                color = MeanColor,
                radius = dotRadius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
            val arm = dotRadius + 4.dp.toPx()
            drawLine(
                color = MeanColor,
                start = Offset(center.x - arm, center.y),
                end = Offset(center.x + arm, center.y),
                strokeWidth = strokeWidth,
            )
            drawLine(
                color = MeanColor,
                start = Offset(center.x, center.y - arm),
                end = Offset(center.x, center.y + arm),
                strokeWidth = strokeWidth,
            )
        }
    }
}
