package com.polar.polarsdkecghrdemo.ui.breathing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingPhase
import com.polar.polarsdkecghrdemo.domain.breathing.BreathingState

private val PacerBlue = Color(0xFF00ACC1)
private val PacerBlueDark = Color(0xFF00838F)

@Composable
fun BreathingSection(viewModel: BreathingPacerViewModel) {
    val breathingState by viewModel.breathingState.collectAsStateWithLifecycle()
    val outToInRatio by viewModel.outToInRatio.collectAsStateWithLifecycle()
    val cycleLengthSeconds by viewModel.cycleLengthSeconds.collectAsStateWithLifecycle()

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

        Spacer(Modifier.height(20.dp))

        ParameterSlider(
            label = "Cycle",
            value = cycleLengthSeconds,
            displayValue = "%.1f s".format(cycleLengthSeconds),
            valueRange = 4f..16f,
            onValueChangeFinished = { viewModel.setCycleLengthSeconds(it) },
        )

        Spacer(Modifier.height(4.dp))

        ParameterSlider(
            label = "Out:In",
            value = outToInRatio,
            displayValue = "%.1f".format(outToInRatio),
            valueRange = 0.5f..4f,
            onValueChangeFinished = { viewModel.setOutToInRatio(it) },
        )
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
        drawCircle(
            color = PacerBlueDark,
            radius = radius,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    displayValue: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderPosition by remember(value) { mutableFloatStateOf(value) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.25f))
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onValueChangeFinished(sliderPosition) },
            valueRange = valueRange,
            modifier = Modifier.weight(0.55f),
        )
        Text(
            displayValue,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.2f),
        )
    }
}
