package dev.upaya.autohrv.ui.breathing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import kotlin.math.PI
import kotlin.math.cos

@Composable
fun BreathingPacerOrb(state: BreathingState, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()

    val scale = when (state.phase) {
        BreathingPhase.Inhale -> 0.5f - 0.5f * cos(PI.toFloat() * state.progress)
        BreathingPhase.Exhale -> 0.5f + 0.5f * cos(PI.toFloat() * state.progress)
    }
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

        // 2. The "Extra" Glow - Dynamic based on orb size
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorScheme.primary.copy(alpha = 0.25f), Color.Transparent),
                center = c,
                radius = orbRadius * 1.4f,
            ),
            radius = orbRadius * 1.4f,
            center = c,
        )

        // 3. The Orb - Material Primary with a 3D-effect gradient
        val gradientCenter = Offset(c.x, c.y - orbRadius * 0.2f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to lerp(colorScheme.primary, Color.White, 0.15f), // Highlight
                    0.5f to colorScheme.primary,                         // Base
                    1.0f to lerp(colorScheme.primary, Color.Black, 0.3f)  // Depth shadow
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

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun BreathingPacerOrbPreview() {
    AutoHrvTheme {
        BreathingPacerOrb(
            state = BreathingState(BreathingPhase.Inhale, 0.6f),
            modifier = Modifier.size(188.dp),
        )
    }
}
