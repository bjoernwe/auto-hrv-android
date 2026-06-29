package dev.upaya.autohrv.ui.hr.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.ui.hr.Sample
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.sin

private const val RR_BASE = 945f

@Composable
fun TimeSeriesChart(samples: List<Sample>, windowMs: Long, modifier: Modifier = Modifier) {
    if (samples.size < 2) return

    val values = samples.map { it.value }
    val minRR = values.min()
    val maxRR = values.max()
    val rangeRR = (maxRR - minRR).coerceAtLeast(1f)

    val nowMs by produceState(System.currentTimeMillis()) {
        while (true) { withFrameMillis { value = System.currentTimeMillis() } }
    }

    val accent = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val padL = 4.dp.toPx()
        val padR = 4.dp.toPx()
        val padT = 16.dp.toPx()
        val padB = 16.dp.toPx()
        val chartW = size.width
        val chartH = size.height
        val plotW = chartW - padL - padR

        fun xFor(t: Long) = padL + (1f - (nowMs - t).toFloat() / windowMs) * plotW
        fun yFor(v: Float) = padT + (1f - (v - minRR) / rangeRR) * (chartH - padT - padB)

        // Baseline dashed line at resting rate
        val baseY = yFor(RR_BASE)
        drawLine(
            color = outlineColor,
            start = Offset(padL, baseY),
            end = Offset(chartW - padR, baseY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())),
        )

        // Build line path
        val path = Path()
        samples.forEachIndexed { i, s ->
            val x = xFor(s.tMillis)
            val y = yFor(s.value)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Area fill
        val areaPath = Path()
        areaPath.addPath(path)
        areaPath.lineTo(xFor(samples.last().tMillis), chartH - padB)
        areaPath.lineTo(xFor(samples.first().tMillis), chartH - padB)
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

        // Beat dots fading older → newer
        samples.forEach { s ->
            val ageFrac = (nowMs - s.tMillis).toFloat() / windowMs
            val alpha = (0.15f + 0.6f * (1f - ageFrac)).coerceIn(0f, 1f)
            drawCircle(
                color = accent.copy(alpha = alpha),
                radius = 1.7.dp.toPx(),
                center = Offset(xFor(s.tMillis), yFor(s.value)),
            )
        }

        // "Now" dot tracks the newest data point
        val latest = samples.last()
        val nowCenter = Offset(xFor(latest.tMillis), yFor(latest.value))
        drawCircle(color = accent.copy(alpha = 0.16f), radius = 10.dp.toPx(), center = nowCenter)
        drawCircle(color = surface, radius = 6.dp.toPx(), center = nowCenter)
        drawCircle(color = accent, radius = 4.2.dp.toPx(), center = nowCenter)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun TimeSeriesChartPreview() {
    AutoHrvTheme {
        val now = System.currentTimeMillis()
        val samples = (0 until 30).map { i ->
            Sample(now - (29 - i) * 1000L, 900f + sin(i * 0.8).toFloat() * 60f)
        }
        TimeSeriesChart(
            samples = samples,
            windowMs = 30_000L,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}
