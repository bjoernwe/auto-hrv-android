package dev.upaya.autohrv.ui.breathing

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.domain.breathing.BreathingState
import dev.upaya.autohrv.ui.theme.AutoHrvAccent
import kotlin.math.PI
import kotlin.math.cos

@Composable
fun BreathingPacerOrb(state: BreathingState, modifier: Modifier = Modifier) {
    val accent = AutoHrvAccent
    val textMeasurer = rememberTextMeasurer()

    val scale = when (state.phase) {
        BreathingPhase.Inhale -> 0.5f - 0.5f * cos(PI.toFloat() * state.progress)
        BreathingPhase.Exhale -> 0.5f + 0.5f * cos(PI.toFloat() * state.progress)
    }
    val label = when (state.phase) {
        BreathingPhase.Inhale -> "Inhale"
        BreathingPhase.Exhale -> "Exhale"
    }

    val labelStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF04181B),
        letterSpacing = 0.04.em,
    )

    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val minFraction = 0.34f
        val orbRadius = maxRadius * (minFraction + (1f - minFraction) * scale)
        val c = center

        // Guide ring
        drawCircle(
            color = accent.copy(alpha = 0.22f),
            radius = maxRadius,
            style = Stroke(width = 1.dp.toPx()),
        )

        // Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                center = c,
                radius = orbRadius * 1.5f,
            ),
            radius = orbRadius * 1.5f,
            center = c,
        )

        // Orb — radial gradient: light centre → accent → dark edge
        val gradientCenter = Offset(c.x, c.y - orbRadius * 0.24f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to lerp(accent, Color.White, 0.15f),
                    0.45f to accent,
                    1.0f to lerp(accent, Color.Black, 0.4f),
                ),
                center = gradientCenter,
                radius = orbRadius,
            ),
            radius = orbRadius,
            center = c,
        )

        // Phase label centred on the orb
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
