package dev.upaya.autohrv.ui.coupling

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

/**
 * Breathing pace indicator, styled as a pill to match the connection chip. The uppercase
 * "INHALE"/"EXHALE" command and a trailing beacon both brighten (toward the primary/teal), and the
 * beacon grows, as the breath value rises on inhale — dimming/shrinking on exhale. The beacon sits on
 * the right so it lands on the same edge as the curve's now-dot, and shares its glow → moat → core
 * construction (see drawNowDot in CouplingHeroCard) so the two live anchors read as the same thing.
 *
 * @param latestBreathValue live breath curve in 0..1 (0 = full exhale, 1 = full inhale).
 */
@Composable
internal fun BreathingChip(
    phase: BreathingPhase,
    latestBreathValue: Float,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val breathColor = MaterialTheme.colorScheme.primary
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val outlineStrong = MaterialTheme.colorScheme.outline

    val v by animateFloatAsState(
        targetValue = latestBreathValue,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "chip-breath-value",
    )
    val breathTint = lerp(onSurface.copy(alpha = 0.35f), breathColor, v)

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, outlineStrong, RoundedCornerShape(999.dp))
                .background(surface2)
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = if (phase == BreathingPhase.Inhale) "INHALE" else "EXHALE",
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = breathTint,
                ),
        )
        // Trailing beacon — same three layers as the curve's now-dot: a soft breath-reactive glow,
        // a surface-colored moat that keeps the core crisp, then the bright core.
        Canvas(Modifier.size(22.dp)) {
            val glowR = (6f + v * 4f).dp.toPx()
            val moatR = (3.5f + v * 2.5f).dp.toPx()
            val coreR = (2f + v * 2f).dp.toPx()
            drawCircle(color = breathColor.copy(alpha = 0.12f + v * 0.12f), radius = glowR, center = center)
            drawCircle(color = surface2, radius = moatR, center = center)
            drawCircle(color = breathTint, radius = coreR, center = center)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "BreathingChip — inhale")
@Composable
private fun BreathingChipInhalePreview() {
    AutoHrvTheme {
        BreathingChip(phase = BreathingPhase.Inhale, latestBreathValue = 0.9f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "BreathingChip — exhale")
@Composable
private fun BreathingChipExhalePreview() {
    AutoHrvTheme {
        BreathingChip(phase = BreathingPhase.Exhale, latestBreathValue = 0.1f)
    }
}
