package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoCorrelationChart(acf: List<Float>, peakLag: Float? = null, modifier: Modifier = Modifier) {
    val lineColor = Color(0xFF00A878)
    val zeroLineColor = Color(0xFF888888)
    val peakColor = Color(0xFF00A878)
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
    ) {
        if (acf.size < 2) return@Canvas
        val yCenter = size.height / 2f
        drawLine(
            color = zeroLineColor,
            start = Offset(0f, yCenter),
            end = Offset(size.width, yCenter),
            strokeWidth = 1.dp.toPx(),
        )
        val strokePx = 2.dp.toPx()
        acf.zipWithNext().forEachIndexed { i, (a, b) ->
            val x0 = i / (acf.size - 1f) * size.width
            val x1 = (i + 1) / (acf.size - 1f) * size.width
            val y0 = yCenter - a.coerceIn(-1f, 1f) * yCenter
            val y1 = yCenter - b.coerceIn(-1f, 1f) * yCenter
            drawLine(
                color = lineColor,
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
        if (peakLag != null) {
            val peakIdx = peakLag.toInt().coerceIn(0, acf.size - 1)
            val px = peakIdx / (acf.size - 1f) * size.width
            val py = yCenter - acf[peakIdx].coerceIn(-1f, 1f) * yCenter
            drawCircle(color = peakColor, radius = 4.dp.toPx(), center = Offset(px, py))
            val label = "%.0f s".format(peakLag)
            val measured = textMeasurer.measure(label, style = TextStyle(fontSize = 12.sp, color = peakColor))
            drawText(
                measured,
                topLeft = Offset(
                    x = (px - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                    y = (py - 4.dp.toPx() - measured.size.height).coerceAtLeast(0f),
                ),
            )
        }
    }
}
