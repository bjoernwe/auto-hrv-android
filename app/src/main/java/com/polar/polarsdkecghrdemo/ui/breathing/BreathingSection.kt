package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPattern
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPhase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState

private val PacerBlue = Color(0xFF00ACC1)
private val PacerBlueDark = Color(0xFF00838F)

@Composable
fun BreathingSection(viewModel: BreathingPacerViewModel) {
    val breathingState by viewModel.breathingState.collectAsStateWithLifecycle(initialValue = null)
    val params by viewModel.currentParams.collectAsStateWithLifecycle()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PacerCircle(state = breathingState)

        Spacer(Modifier.height(12.dp))

        Text(
            text = when (breathingState?.phase) {
                BreathingPhase.Inhale -> "Inhale"
                BreathingPhase.Exhale -> "Exhale"
                null -> "—"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = PacerBlueDark,
        )

        Spacer(Modifier.height(12.dp))

        ParamReadout(params)
    }
}

@Composable
private fun ParamReadout(params: BreathingPattern) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ParamLabel(label = "Cycle", value = "%.1f s".format(params.cycleLengthSeconds))
        Spacer(Modifier.width(24.dp))
        ParamLabel(label = "Out:In", value = "%.1f".format(params.outToInRatio))
    }
}

@Composable
private fun ParamLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PacerCircle(state: BreathingState?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(200.dp)) {
        val maxRadius = size.minDimension * 0.45f
        val minRadius = size.minDimension * 0.18f

        val fraction = when (state?.phase) {
            BreathingPhase.Inhale -> state.progress
            BreathingPhase.Exhale -> 1f - state.progress
            null -> 0f
        }
        val radius = lerp(minRadius, maxRadius, fraction)

        drawCircle(color = PacerBlue.copy(alpha = 0.25f), radius = maxRadius)
        drawCircle(color = PacerBlue.copy(alpha = 0.7f), radius = radius)
        drawCircle(color = PacerBlueDark, radius = radius, style = Stroke(width = 2.dp.toPx()))
    }
}
