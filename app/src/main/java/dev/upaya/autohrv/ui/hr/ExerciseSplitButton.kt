package dev.upaya.autohrv.ui.hr

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.domain.breathing.Exercise

private val EXERCISES = listOf(Exercise.SlowResonance, Exercise.FastResonance, Exercise.Sweep)

// SplitButtonLayout measures its leading/trailing buttons with loose constraints and only
// stretches its own outer bounds to fill available width, leaving the buttons at their natural
// size with dead space beside them. A Row with weight(1f) on the leading button grows it into
// the available width instead, using the same SplitButtonDefaults styling.
@Composable
fun ExerciseSplitButton(
    selected: Exercise,
    isRunning: Boolean,
    onSelect: (Exercise) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "exercise-menu-chevron")

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(SplitButtonDefaults.Spacing),
        ) {
            SplitButtonDefaults.LeadingButton(
                onClick = onToggle,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isRunning) "Stop" else selected.label)
            }
            Box {
                SplitButtonDefaults.TrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (expanded) "Collapse exercise menu" else "Expand exercise menu",
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
                ExerciseMenu(
                    expanded = expanded,
                    selected = selected,
                    onSelect = onSelect,
                    onDismiss = { expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ExerciseMenu(
    expanded: Boolean,
    selected: Exercise,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        EXERCISES.forEach { exercise ->
            DropdownMenuItem(
                text = { Text(exercise.label) },
                onClick = {
                    onSelect(exercise)
                    onDismiss()
                },
                trailingIcon = { if (exercise == selected) Icon(Icons.Filled.Check, contentDescription = null) },
            )
        }
    }
}
