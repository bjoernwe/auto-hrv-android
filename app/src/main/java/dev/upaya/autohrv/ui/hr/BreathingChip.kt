package dev.upaya.autohrv.ui.hr

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

/**
 * Breathing pace indicator, styled as a pill to match the connection chip. A leading dot and the
 * "inhale"/"exhale" word both brighten (toward the primary/teal) and the dot grows as the breath
 * value rises on inhale, dimming/shrinking on exhale — a temporal anchor mirroring the now-dot.
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

    val animatedBreathValue by animateFloatAsState(
        targetValue = latestBreathValue,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "chip-breath-value",
    )
    val breathTint = lerp(onSurface.copy(alpha = 0.35f), breathColor, animatedBreathValue)

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, outlineStrong, RoundedCornerShape(999.dp))
                .background(surface2)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .scale(0.7f + animatedBreathValue * 0.5f)
                .background(breathTint, CircleShape),
        )
        Text(
            text = if (phase == BreathingPhase.Inhale) "inhale" else "exhale",
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = breathTint,
                ),
        )
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
