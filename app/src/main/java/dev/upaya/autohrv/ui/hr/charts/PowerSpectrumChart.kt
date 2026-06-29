package dev.upaya.autohrv.ui.hr.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
fun PowerSpectrumChart(spectrum: List<Float>, modifier: Modifier = Modifier) {
    val barColor = Color(0xFF0078D4)
    Canvas(
        modifier = modifier,
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

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun PowerSpectrumChartPreview() {
    AutoHrvTheme {
        val spectrum = (0 until 50).map { i -> 1f / (1f + (i - 15) * (i - 15) * 0.1f) }
        PowerSpectrumChart(
            spectrum = spectrum,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}
