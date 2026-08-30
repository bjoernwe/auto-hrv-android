package dev.upaya.autohrv.ui.hr.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.domain.spectral.SpectrogramSlice
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_POWER = 1e-6f
private const val MIN_FREQ_RANGE_HZ = 1e-6f

// Evenly spaced axis ticks across the displayed range: min, two intermediate, max.
private val AXIS_TICK_FRACTIONS = listOf(0f, 1f / 3f, 2f / 3f, 1f)

/**
 * Time × frequency × power heatmap. Columns are slices, oldest at left / newest at right (matching
 * [TimeSeriesChart]'s convention); rows are frequency bins, lowest at the bottom.
 *
 * Color intensity is normalized against the loudest bin currently on screen, then square-root
 * compressed — power spectra span orders of magnitude, so a raw linear map would leave everything
 * but the single loudest cell looking empty.
 */
@Composable
fun SpectrogramChart(
    slices: List<SpectrogramSlice>,
    freqBinsHz: List<Float>,
    mayerBandHz: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    if (slices.isEmpty() || freqBinsHz.isEmpty()) return

    val surface = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.secondary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = muted)
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(color = muted.copy(alpha = 0.55f))

    val maxPower = (slices.maxOfOrNull { it.powerByFreqBin.maxOrNull() ?: 0f } ?: 0f).coerceAtLeast(MIN_POWER)
    val minFreq = freqBinsHz.first()
    val maxFreq = freqBinsHz.last()
    val freqRange = (maxFreq - minFreq).coerceAtLeast(MIN_FREQ_RANGE_HZ)

    Canvas(modifier = modifier) {
        val padL = 30.dp.toPx()
        val padR = 32.dp.toPx()
        val padT = 6.dp.toPx()
        val padB = 6.dp.toPx()
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB
        val colW = plotW / slices.size
        val rowH = plotH / freqBinsHz.size

        slices.forEachIndexed { col, slice ->
            val x = padL + col * colW
            freqBinsHz.indices.forEach { row ->
                val power = slice.powerByFreqBin.getOrElse(row) { 0f }
                val t = sqrt((power / maxPower).coerceIn(0f, 1f))
                // Row 0 is the lowest frequency, drawn at the bottom of the plot.
                val y = padT + plotH - (row + 1) * rowH
                drawRect(
                    color = lerp(surface, accent, t),
                    topLeft = Offset(x, y),
                    size = Size(colW, rowH),
                )
            }
        }

        fun yForFreq(hz: Float) = padT + plotH * (1f - (hz - minFreq) / freqRange)

        // Cycle length (1/f), matching the ACF chart's peak-lag convention, since "seconds per
        // cycle" reads more intuitively here than a Hz value.
        fun cycleLengthLabel(hz: Float) = "%.0fs".format(1f / hz)

        fun drawLabelAt(
            hz: Float,
            style: TextStyle,
            alignRight: Boolean,
        ) {
            val y = yForFreq(hz)
            val measured = textMeasurer.measure(cycleLengthLabel(hz), style = style)
            val x = if (alignRight) size.width - padR + 4.dp.toPx() else padL - 4.dp.toPx() - measured.size.width
            drawText(measured, topLeft = Offset(x, y - measured.size.height / 2f))
        }

        // Plain axis ticks (no line) on the left: min, two intermediate, max — orients the reader
        // on the frequency scale without competing visually with the Mayer-band callouts on the
        // right. Bin 0 (DC) is never part of freqBinsHz (see frequencyBinIndicesIn), so every tick
        // here has hz > 0 and a well-defined cycle length — the slowest one equals the window length.
        AXIS_TICK_FRACTIONS.forEach { fraction ->
            drawLabelAt(minFreq + fraction * freqRange, axisLabelStyle, alignRight = false)
        }

        listOf(mayerBandHz.start, mayerBandHz.endInclusive).forEach { hz ->
            if (hz in minFreq..maxFreq) {
                val y = yForFreq(hz)
                drawLine(
                    color = muted.copy(alpha = 0.5f),
                    start = Offset(padL, y),
                    end = Offset(size.width - padR, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())),
                )
                drawLabelAt(hz, labelStyle, alignRight = true)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun SpectrogramChartPreview() {
    AutoHrvTheme {
        val freqBins = (0..25).map { it * 0.008f }
        val now = System.currentTimeMillis()
        val slices =
            (0 until 60).map { i ->
                SpectrogramSlice(
                    timestampMillis = now - (59 - i) * 10_000L,
                    powerByFreqBin =
                        freqBins.map { hz ->
                            val mayerPeak = exp(-((hz - 0.09f) * (hz - 0.09f)) / 0.0008f)
                            val drift = 0.5f + 0.5f * sin(i * 0.05f)
                            (mayerPeak * drift * 100f).coerceAtLeast(0.5f)
                        },
                )
            }
        SpectrogramChart(
            slices = slices,
            freqBinsHz = freqBins,
            mayerBandHz = 0.04f..0.15f,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
        )
    }
}
