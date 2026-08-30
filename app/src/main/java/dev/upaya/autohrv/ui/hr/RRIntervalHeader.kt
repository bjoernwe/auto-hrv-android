package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.roundToInt

@Composable
internal fun RRIntervalHeader(swing: Int?) {
    val accent = MaterialTheme.colorScheme.secondary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val faint = muted.copy(alpha = 0.6f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            SectionLabel(text = "R–R INTERVAL")
            Spacer(Modifier.height(2.dp))
            Text(
                text = "beat-to-beat, ms",
                style = MaterialTheme.typography.labelMedium.copy(color = muted),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = swing?.let { "$it" } ?: "—",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        ),
                )
                Text(
                    text = "ms",
                    style = MaterialTheme.typography.labelMedium.copy(color = muted),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = "SWING",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        letterSpacing = 0.1.em,
                        color = faint,
                    ),
            )
        }
    }
}

@Composable
internal fun ACFHeader() = ChartSectionHeader(label = "AUTOCORRELATION", subtitle = "peak ➔ pace")

@Composable
internal fun SpectrogramHeader() = ChartSectionHeader(label = "SPECTROGRAM", subtitle = "fast · mayer · slow")

@Composable
private fun ChartSectionHeader(
    label: String,
    subtitle: String,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = label)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, color = muted),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BandRangeSlider(
    value: IntRange,
    onValueChange: (IntRange) -> Unit,
    valueRange: IntRange,
    allowedRange: IntRange,
    enabled: Boolean = true,
) {
    val accent = MaterialTheme.colorScheme.primary
    val safeValueRange = if (valueRange.first < valueRange.last) valueRange else 0..1
    val lo = value.first.coerceIn(allowedRange).coerceIn(safeValueRange)
    val hi = value.last.coerceIn(allowedRange).coerceIn(safeValueRange)

    val sliderColors =
        SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = acfInRangeColor(),
            inactiveTrackColor = acfOutRangeColor(),
        )

    Column {
        val intSteps = maxOf(0, (safeValueRange.last - safeValueRange.first) - 1)
        RangeSlider(
            // Compose's RangeSlider works in Float; the band is integer lags by type, so we
            // convert to Float here and round back to whole seconds in onValueChange.
            value = lo.toFloat()..hi.toFloat(),
            onValueChange = { new ->
                val newLo = new.start.roundToInt().coerceIn(allowedRange)
                val newHi = new.endInclusive.roundToInt().coerceIn(allowedRange)
                onValueChange(newLo..newHi)
            },
            valueRange = safeValueRange.first.toFloat()..safeValueRange.last.toFloat(),
            steps = intSteps,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(top = 2.dp, start = 4.dp, end = 4.dp),
            colors = sliderColors,
            startThumb = { ThumbWithLabel(label = lo.toString(), accent = accent) },
            endThumb = { ThumbWithLabel(label = hi.toString(), accent = accent) },
            track = { rangeSliderState ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                ) {
                    SliderDefaults.Track(
                        rangeSliderState = rangeSliderState,
                        modifier = Modifier.height(2.dp),
                        colors = sliderColors,
                        drawTick = { _, _ -> },
                    )
                }
            },
        )
    }
}

@Composable
private fun ThumbWithLabel(
    label: String,
    accent: Color,
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.wrapContentSize(unbounded = true),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = labelColor),
            modifier = Modifier.offset(y = (-22).dp),
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .shadow(2.dp, CircleShape)
                        .background(accent, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "RRIntervalHeader — swing")
@Composable
private fun RRIntervalHeaderPreview() {
    AutoHrvTheme {
        HrvCard { RRIntervalHeader(swing = 284) }
    }
}
