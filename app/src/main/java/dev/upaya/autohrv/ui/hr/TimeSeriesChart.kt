package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.sin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val RR_BASE = 945f

@Composable
fun TimeSeriesChart(ts: List<Int>, modifier: Modifier = Modifier) {
    if (ts.size < 2) return

    val minRR = ts.min().toFloat()
    val maxRR = ts.max().toFloat()
    val rangeRR = (maxRR - minRR).coerceAtLeast(1f)

    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier,
    ) {
        val padL = 4.dp.toPx()
        val padR = 4.dp.toPx()
        val padT = 16.dp.toPx()
        val padB = 16.dp.toPx()
        val chartW = size.width
        val chartH = size.height

        val n = ts.size
        val xs = { i: Int -> padL + (i.toFloat() / (n - 1).coerceAtLeast(1)) * (chartW - padL - padR) }
        val ys = { rr: Int ->
            padT + (1f - (rr.toFloat() - minRR) / rangeRR) * (chartH - padT - padB)
        }

        // Baseline dashed line at resting rate
        val baseY = ys(RR_BASE.toInt())
        drawLine(
            color = outlineColor,
            start = Offset(padL, baseY),
            end = Offset(chartW - padR, baseY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())),
        )

        // Build line path
        val path = Path()
        ts.forEachIndexed { i, rr ->
            val x = xs(i)
            val y = ys(rr)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Area fill — vertical gradient under the curve
        val areaPath = Path()
        areaPath.addPath(path)
        areaPath.lineTo(xs(n - 1), chartH - padB)
        areaPath.lineTo(xs(0), chartH - padB)
        areaPath.close()
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0f)),
                startY = padT,
                endY = chartH - padB,
            ),
        )

        // Line — horizontal fade: transparent at left → opaque at right
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to accent.copy(alpha = 0f),
                    0.12f to accent.copy(alpha = 0.9f),
                    1f to accent,
                ),
                startX = 0f,
                endX = chartW,
            ),
            style = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Beat dots fading left → right
        ts.forEachIndexed { i, rr ->
            val alpha = 0.15f + 0.6f * (i.toFloat() / (n - 1).coerceAtLeast(1))
            drawCircle(
                color = accent.copy(alpha = alpha),
                radius = 1.7.dp.toPx(),
                center = Offset(xs(i), ys(rr)),
            )
        }

        // "Now" dot at rightmost point
        val lastX = xs(n - 1)
        val lastY = ys(ts.last())
        val nowCenter = Offset(lastX, lastY)
        drawCircle(color = accent.copy(alpha = 0.16f), radius = 10.dp.toPx(), center = nowCenter)
        drawCircle(color = surface, radius = 6.dp.toPx(), center = nowCenter)
        drawCircle(color = accent, radius = 4.2.dp.toPx(), center = nowCenter)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun TimeSeriesChartPreview() {
    AutoHrvTheme {
        val ts = (0 until 30).map { i -> (900 + (sin(i * 0.8) * 60).toInt()) }
        TimeSeriesChart(
            ts = ts,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}
