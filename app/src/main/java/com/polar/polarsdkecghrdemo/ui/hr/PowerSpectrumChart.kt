package com.polar.polarsdkecghrdemo.ui.hr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PowerSpectrumChart(spectrum: List<Float>, modifier: Modifier = Modifier) {
    val barColor = Color(0xFF0078D4)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
    ) {
        if (spectrum.isEmpty()) return@Canvas
        val maxPower = spectrum.max().coerceAtLeast(1e-6f)
        val gap = 1.dp.toPx()
        val barWidth = (size.width / spectrum.size) - gap

        spectrum.forEachIndexed { i, power ->
            val barHeight = (power / maxPower) * size.height
            drawRect(
                color = barColor,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
            )
        }
    }
}