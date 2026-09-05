package dev.upaya.autohrv.ui.hr.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.domain.spectral.SpectrogramSlice
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.sin

private const val MIN_POWER = 1e-6f

/** One band's data to draw: a label, its rolling slices, and the Hz of each frequency bin. */
data class SpectrogramBandView(
    val label: String,
    val slices: List<SpectrogramSlice>,
    val freqBinsHz: List<Float>,
)

/**
 * Time × frequency × power heatmap, split into one stacked sub-panel per frequency band (fastest on
 * top, slowest at the bottom) with a gap between them. Within each panel, columns are slices —
 * oldest at left / newest at right (matching [TimeSeriesChart]'s convention) — and rows are
 * frequency bins, lowest at the bottom.
 *
 * Color intensity is normalized **per band** against the loudest bin currently on screen in that
 * band, then square-root compressed — power spectra span orders of magnitude and low frequencies
 * dominate, so a single shared normalization would leave the faster bands looking empty.
 */
@Composable
fun SpectrogramChart(
    bands: List<SpectrogramBandView>,
    modifier: Modifier = Modifier,
) {
    if (bands.none { it.slices.isNotEmpty() && it.freqBinsHz.isNotEmpty() }) return

    val surface = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.secondary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(color = muted.copy(alpha = 0.55f))

    // Highest frequency on top, derived from the data rather than the caller's order. Each panel is
    // normalized against its own loudest bin (a display decision — see the class doc); computed here
    // in the composition, not in the draw lambda, so it re-runs on data change rather than per frame.
    val panels = bands.sortedByDescending { it.freqBinsHz.firstOrNull() ?: 0f }
    val maxPowers =
        panels.map { band ->
            (band.slices.maxOfOrNull { it.powerByFreqBin.maxOrNull() ?: 0f } ?: 0f).coerceAtLeast(MIN_POWER)
        }

    Canvas(modifier = modifier) {
        val padL = 30.dp.toPx()
        val padR = 6.dp.toPx()
        val padT = 6.dp.toPx()
        val padB = 6.dp.toPx()
        val gap = 8.dp.toPx()
        val plotW = size.width - padL - padR
        // Panels split the plot height evenly, with a gap between each.
        val totalPlotH = size.height - padT - padB - gap * (panels.size - 1)
        val panelH = totalPlotH / panels.size

        // Cycle length (1/f), matching the ACF chart's peak-lag convention, since "seconds per
        // cycle" reads more intuitively here than a Hz value.
        fun cycleLengthLabel(hz: Float) = "%.0fs".format(1f / hz)

        fun drawLabel(
            text: String,
            centerY: Float,
            style: TextStyle,
        ) {
            val measured = textMeasurer.measure(text, style = style)
            val x = padL - 4.dp.toPx() - measured.size.width
            drawText(measured, topLeft = Offset(x, centerY - measured.size.height / 2f))
        }

        // Minimum vertical spacing between tick labels so they never overlap.
        val minTickSpacing = 16.dp.toPx()

        panels.forEachIndexed { panelIndex, band ->
            val panelTop = padT + panelIndex * (panelH + gap)
            if (band.slices.isEmpty() || band.freqBinsHz.isEmpty()) {
                // Band not yet activated (its window is still filling): leave the panel empty.
                return@forEachIndexed
            }

            val invMaxPower = 1f / maxPowers[panelIndex]
            val colW = plotW / band.slices.size
            val rowH = panelH / band.freqBinsHz.size
            val cellSize = Size(colW, rowH)

            band.slices.forEachIndexed { col, slice ->
                val x = padL + col * colW
                band.freqBinsHz.indices.forEach { row ->
                    val power = slice.powerByFreqBin.getOrElse(row) { 0f }
                    val t = log10(1f + 9f * (power * invMaxPower).coerceIn(0f, 1f))
                    // Row 0 is the lowest frequency, drawn at the bottom of the panel.
                    val y = panelTop + panelH - (row + 1) * rowH
                    drawRect(
                        color = lerp(surface, accent, t),
                        topLeft = Offset(x, y),
                        size = cellSize,
                    )
                }
            }

            // Cycle-length ticks from the slowest (bottom) to the fastest (top) bin of this band.
            // As many intermediate ticks as fit without crowding, evenly spaced by row index —
            // row index maps linearly to y, so this keeps labels aligned with the rows they name.
            val lastRow = band.freqBinsHz.size - 1
            if (lastRow == 0) {
                drawLabel(cycleLengthLabel(band.freqBinsHz[0]), panelTop + panelH / 2f, axisLabelStyle)
            } else {
                val tickCount = (panelH / minTickSpacing).toInt().coerceIn(1, lastRow)
                (0..tickCount).forEach { i ->
                    val row = i * lastRow / tickCount
                    val y = panelTop + panelH * (1f - row.toFloat() / lastRow)
                    drawLabel(cycleLengthLabel(band.freqBinsHz[row]), y, axisLabelStyle)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun SpectrogramChartPreview() {
    AutoHrvTheme {
        val now = System.currentTimeMillis()

        fun band(
            label: String,
            bins: List<Float>,
            peakHz: Float,
            count: Int,
        ) = SpectrogramBandView(
            label = label,
            freqBinsHz = bins,
            slices =
                (0 until count).map { i ->
                    SpectrogramSlice(
                        timestampMillis = now - (count - 1 - i) * 10_000L,
                        powerByFreqBin =
                            bins.map { hz ->
                                val peak = exp(-((hz - peakHz) * (hz - peakHz)) / 0.0008f)
                                val drift = 0.5f + 0.5f * sin(i * 0.05f)
                                (peak * drift * 100f).coerceAtLeast(0.5f)
                            },
                    )
                },
        )

        SpectrogramChart(
            bands =
                listOf(
                    band("SLOW", (1..5).map { it * 0.008f }, peakHz = 0.02f, count = 40),
                    band("MAYER", (3..9).map { it / 64f }, peakHz = 0.09f, count = 60),
                    band("FAST", (5..12).map { it / 32f }, peakHz = 0.25f, count = 90),
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
        )
    }
}
