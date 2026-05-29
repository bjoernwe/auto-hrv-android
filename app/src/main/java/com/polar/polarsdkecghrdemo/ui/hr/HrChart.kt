package com.polar.polarsdkecghrdemo.ui.hr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun HrChart(hrHistory: List<Int>, modifier: Modifier = Modifier) {
    val lineColor = Color(0xFFC00000)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
    ) {
        val min = hrHistory.min().toFloat()
        val max = hrHistory.max().toFloat()
        val range = (max - min).coerceAtLeast(10f)
        val strokePx = 2.dp.toPx()

        hrHistory.zipWithNext().forEachIndexed { i, (a, b) ->
            val x0 = i / (hrHistory.size - 1f) * size.width
            val x1 = (i + 1) / (hrHistory.size - 1f) * size.width
            val y0 = size.height - (a - min) / range * size.height
            val y1 = size.height - (b - min) / range * size.height
            drawLine(
                color = lineColor,
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}
