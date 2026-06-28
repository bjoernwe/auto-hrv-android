package dev.upaya.autohrv.ui.breathing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.domain.breathing.BreathingState

@Composable
fun BreathingPacerOrb(
    state: BreathingState,
    modifier: Modifier = Modifier,
    inResonance: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()

    // Convergence: when heart and breath lock into resonance, the warm heart
    // tone rises into the cool breath orb — the two systems meeting at its edge.
    val convergence by animateFloatAsState(
        targetValue = if (inResonance) 1f else 0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "orb-convergence",
    )
    val breath = colorScheme.primary
    val heart = colorScheme.secondary

    val scale = state.value
    val label = when (state.phase) {
        BreathingPhase.Inhale -> "inhale"
        BreathingPhase.Exhale -> "exhale"
    }

    val labelStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        color = colorScheme.onPrimary,
        letterSpacing = 0.02.em,
    )

    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val minFraction = 0.4f
        val orbRadius = maxRadius * (minFraction + (1f - minFraction) * scale)
        val c = center

        // 1. Guide ring (Material-consistent boundary)
        drawCircle(
            color = colorScheme.outlineVariant.copy(alpha = 0.3f),
            radius = maxRadius,
            style = Stroke(width = 1.dp.toPx()),
        )

        // 2. The glow — warms toward the heart tone and widens as resonance locks in.
        val glowColor = lerp(breath, heart, convergence * 0.55f)
        val glowRadius = orbRadius * (1.4f + 0.15f * convergence)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.25f + 0.10f * convergence), Color.Transparent),
                center = c,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = c,
        )

        // 3. The orb — a cool breath core whose rim warms toward the heart tone
        //    as the two systems converge, with a 3D-effect gradient.
        val core = lerp(breath, heart, convergence * 0.12f)
        val rim = lerp(lerp(breath, Color.Black, 0.3f), heart, convergence * 0.5f)
        val gradientCenter = Offset(c.x, c.y - orbRadius * 0.2f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to lerp(core, Color.White, 0.15f), // Highlight
                    0.5f to core,                           // Base
                    1.0f to rim,                            // Depth / warm rim
                ),
                center = gradientCenter,
                radius = orbRadius,
            ),
            radius = orbRadius,
            center = c,
        )

        // 4. Phase label centred on the orb
        val measured = textMeasurer.measure(label, style = labelStyle)
        drawText(
            measured,
            topLeft = Offset(
                c.x - measured.size.width / 2f,
                c.y - measured.size.height / 2f,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Orb — tuning")
@Composable
private fun BreathingPacerOrbPreview() {
    AutoHrvTheme {
        BreathingPacerOrb(
            state = BreathingState(BreathingPhase.Inhale, 0.6f),
            modifier = Modifier.size(188.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Orb — in resonance")
@Composable
private fun BreathingPacerOrbResonancePreview() {
    AutoHrvTheme {
        BreathingPacerOrb(
            state = BreathingState(BreathingPhase.Inhale, 0.6f),
            modifier = Modifier.size(188.dp),
            inResonance = true,
        )
    }
}
