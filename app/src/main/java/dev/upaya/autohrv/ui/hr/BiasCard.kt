package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.log2
import kotlin.math.pow

// The BIAS slider is a log2-scaled view onto the out-to-in ratio: -1 ("fast") is a short,
// energizing exhale, 0 is the symmetric default, +1 ("slow") is an extended, calming exhale.
private const val MIN_OUT_TO_IN_RATIO = 0.5f
private const val MAX_OUT_TO_IN_RATIO = 2f

private fun ratioToBias(ratio: Float): Float =
    log2(ratio.coerceIn(MIN_OUT_TO_IN_RATIO, MAX_OUT_TO_IN_RATIO)).coerceIn(-1f, 1f)

private fun biasToRatio(bias: Float): Float = 2f.pow(bias.coerceIn(-1f, 1f))

@Composable
internal fun BiasCard(
    outToInRatio: Float,
    onOutToInRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    HrvCard(modifier = modifier) {
        BiasHeader()
        Spacer(Modifier.height(10.dp))
        BiasSlider(
            bias = ratioToBias(outToInRatio),
            onBiasChange = { onOutToInRatioChange(biasToRatio(it)) },
        )
    }
}

@Composable
private fun BiasHeader() {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = "BIAS")
        Text(
            text = "inhale ↔ exhale",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, color = muted),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BiasSlider(
    bias: Float,
    onBiasChange: (Float) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val sliderColors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent.copy(alpha = 0.38f),
        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
    )

    Column {
        Slider(
            value = bias,
            onValueChange = onBiasChange,
            valueRange = -1f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(top = 2.dp),
            colors = sliderColors,
            thumb = { BiasThumb(accent = accent) },
            track = { sliderState ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                ) {
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(2.dp),
                        colors = sliderColors,
                        drawTick = { _, _ -> },
                    )
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "energizing",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, color = labelColor),
            )
            Text(
                text = "calming",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, color = labelColor),
            )
        }
    }
}

@Composable
private fun BiasThumb(accent: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(12.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .shadow(2.dp, CircleShape)
                .background(accent, CircleShape),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Bias — centered")
@Composable
private fun BiasCardPreview() {
    var ratio by remember { mutableFloatStateOf(1f) }
    AutoHrvTheme {
        BiasCard(outToInRatio = ratio, onOutToInRatioChange = { ratio = it })
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Bias — slow exhale")
@Composable
private fun BiasCardSlowPreview() {
    AutoHrvTheme {
        BiasCard(outToInRatio = 1.6f, onOutToInRatioChange = {})
    }
}
