package dev.upaya.autohrv.ui.hr

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
internal fun ResonancePill(
    cycleLengthSec: Float,
    breathsPerMin: Float?,
    lagSeconds: Float?,
) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val heart = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .background(surface)
            .padding(start = 16.dp, end = 16.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillNumber(value = "%.1f".format(cycleLengthSec), unit = "s", onSurface = onSurface, muted = muted)

        PillDivider(outline)

        PillNumber(
            value = breathsPerMin?.let { "%.1f".format(it) } ?: "—",
            unit = "/min",
            onSurface = onSurface,
            muted = muted,
        )

        PillDivider(outline)

        // τ lag readout — shown in heart color; "—" until cross-correlation is implemented
        PillNumber(
            value = lagSeconds?.let { "%.0f".format(it) } ?: "—",
            unit = "s lag",
            onSurface = onSurface,
            muted = muted,
            valueColor = heart,
        )
    }
}

@Composable
private fun PillNumber(
    value: String,
    unit: String,
    onSurface: Color,
    muted: Color,
    valueColor: Color = onSurface,
) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            ),
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium.copy(color = muted),
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

@Composable
private fun PillDivider(color: Color) {
    Spacer(Modifier.width(13.dp))
    Box(Modifier.width(1.dp).height(18.dp).background(color))
    Spacer(Modifier.width(13.dp))
}

@Composable
internal fun ResonanceChip(isInResonance: Boolean) {
    val accent = if (isInResonance) MaterialTheme.colorScheme.primary
                 else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (isInResonance) accent.copy(alpha = 0.10f)
                     else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(6.dp)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ResonanceBeacon(accent = accent, pulsing = isInResonance)
        Text(
            text = if (isInResonance) "LOCKED" else "TUNING",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = accent,
            ),
        )
    }
}

@Composable
private fun ResonanceBeacon(accent: Color, pulsing: Boolean) {
    val dotSize = 7.dp
    val beaconSize = 18.dp

    if (!pulsing) {
        Box(Modifier.size(beaconSize), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dotSize).background(accent, CircleShape))
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "resonance-beacon")
    val glowProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val dotScale by transition.animateFloat(
        initialValue = 0.82f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot",
    )
    val ringScale by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1200),
        ),
        label = "ring-scale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.65f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1200),
        ),
        label = "ring-alpha",
    )

    Box(Modifier.size(beaconSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(beaconSize)
                .scale(0.7f + glowProgress * 0.6f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.45f * (1f - glowProgress * 0.7f)),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(beaconSize)
                .scale(ringScale)
                .border(1.dp, accent.copy(alpha = ringAlpha), CircleShape),
        )
        Box(
            Modifier
                .size(beaconSize)
                .scale(dotScale * (dotSize.value / beaconSize.value))
                .background(accent, CircleShape),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun ResonancePillPreview() {
    AutoHrvTheme {
        ResonancePill(
            cycleLengthSec = 10.8f,
            breathsPerMin = 5.6f,
            lagSeconds = 2.3f,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun ResonancePillNoLagPreview() {
    AutoHrvTheme {
        ResonancePill(
            cycleLengthSec = 10.0f,
            breathsPerMin = 6.0f,
            lagSeconds = null,
        )
    }
}
