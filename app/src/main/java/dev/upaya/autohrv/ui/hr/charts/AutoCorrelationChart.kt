package dev.upaya.autohrv.ui.hr.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp

@Composable
fun AutoCorrelationChart(
    acf: List<Float>,
    modifier: Modifier = Modifier,
    peakLag: Float? = null,
    bandLo: Float = 0f,
    bandHi: Float = Float.MAX_VALUE,
) {
    if (acf.size < 2) return

    // The ACF curve is heart-derived → warm tone. The peak and band — which set
    // the breathing pace — use the cool breath tone. "peak → pace" made literal.
    val heart = MaterialTheme.colorScheme.secondary
    val breath = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val bgColor = MaterialTheme.colorScheme.background
    val textMeasurer = rememberTextMeasurer()

    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        color = breath,
    )
    Canvas(
        modifier = modifier,
    ) {
        val padL = 10.dp.toPx()
        val padR = 10.dp.toPx()
        val padT = 14.dp.toPx()
        val padB = 22.dp.toPx()
        val chartW = size.width
        val chartH = size.height
        val plotW = chartW - padL - padR
        val plotH = chartH - padT - padB

        val maxLag = (acf.size - 1).toFloat()
        val xs = { t: Float -> padL + (t / maxLag).coerceIn(0f, 1f) * plotW }
        val yCenter = padT + plotH / 2f
        val yHalf = plotH / 2f
        val ys = { v: Float -> yCenter - v.coerceIn(-1f, 1f) * yHalf }

        // Out-of-band shading (regions outside [bandLo, bandHi])
        val bx0 = xs(bandLo.coerceAtMost(maxLag))
        val bx1 = xs(bandHi.coerceAtMost(maxLag))
        val shadingColor = bgColor.copy(alpha = 0.52f)
        if (bx0 > padL) {
            drawRect(
                color = shadingColor,
                topLeft = Offset(padL, padT),
                size = Size(bx0 - padL, plotH),
            )
        }
        if (bx1 < chartW - padR) {
            drawRect(
                color = shadingColor,
                topLeft = Offset(bx1, padT),
                size = Size(chartW - padR - bx1, plotH),
            )
        }

        // Band edge dashed lines
        val bandEdgeDash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
        if (bandLo > 0f && bandLo <= maxLag) {
            drawLine(
                color = breath.copy(alpha = 0.35f),
                start = Offset(bx0, padT),
                end = Offset(bx0, chartH - padB),
                strokeWidth = 1.dp.toPx(),
                pathEffect = bandEdgeDash,
            )
        }
        if (bandHi < maxLag) {
            drawLine(
                color = breath.copy(alpha = 0.35f),
                start = Offset(bx1, padT),
                end = Offset(bx1, chartH - padB),
                strokeWidth = 1.dp.toPx(),
                pathEffect = bandEdgeDash,
            )
        }

        // Zero line
        drawLine(
            color = outlineColor,
            start = Offset(padL, yCenter),
            end = Offset(chartW - padR, yCenter),
            strokeWidth = 1.dp.toPx(),
        )

        // ACF curve
        val path = Path()
        acf.forEachIndexed { i, v ->
            val x = xs(i.toFloat())
            val y = ys(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = heart,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Peak marker
        if (peakLag != null) {
            val peakIdx = peakLag.toInt().coerceIn(0, acf.size - 1)
            val peakX = xs(peakIdx.toFloat())
            val peakY = ys(acf[peakIdx])

            // Dashed vertical line at peak
            drawLine(
                color = breath.copy(alpha = 0.45f),
                start = Offset(peakX, padT),
                end = Offset(peakX, chartH - padB),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
            )

            // Dot on curve (surface ring + breath dot — this is the chosen pace)
            drawCircle(color = surface, radius = 6.5.dp.toPx(), center = Offset(peakX, peakY))
            drawCircle(color = breath, radius = 4.5.dp.toPx(), center = Offset(peakX, peakY))

            // Label above the marker
            val peakLabel = "%.0fs".format(peakLag)
            val measured = textMeasurer.measure(peakLabel, style = labelStyle)
            drawText(
                measured,
                topLeft = Offset(
                    (peakX - measured.size.width / 2f).coerceIn(padL, chartW - padR - measured.size.width),
                    padT + 2.dp.toPx(),
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun AutoCorrelationChartPreview() {
    AutoHrvTheme {
        val acf = (0..60).map { i ->
            (cos(2 * PI * i / 10.0) * exp(-i * 0.05)).toFloat()
        }
        AutoCorrelationChart(
            acf = acf,
            peakLag = 10f,
            bandLo = 7f,
            bandHi = 13f,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}
