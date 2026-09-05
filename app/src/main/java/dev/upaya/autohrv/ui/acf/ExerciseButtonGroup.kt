package dev.upaya.autohrv.ui.acf

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.upaya.autohrv.domain.breathing.Exercise

@Composable
fun ExerciseButtonGroup(
    activeRange: IntRange,
    onSelect: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = Exercise.all
    // These segments set the same target cycle-length range as the BandRangeSlider above, so they
    // share its breath-side identity: a quiet teal-tinted fill with full-strength teal content,
    // matching the ResonanceChip "locked" idiom rather than Material's default secondaryContainer
    // (an undefined-in-theme lavender that clashes with the two-tone palette).
    val breath = MaterialTheme.colorScheme.primary
    val colors =
        SegmentedButtonDefaults.colors(
            activeContainerColor = breath.copy(alpha = 0.12f),
            activeContentColor = breath,
            activeBorderColor = breath.copy(alpha = 0.22f),
            inactiveContainerColor = Color.Transparent,
            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
        )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        exercises.forEachIndexed { index, exercise ->
            val isActive = exercise.cycleLengthRange == activeRange
            SegmentedButton(
                selected = isActive,
                onClick = { if (!isActive) onSelect(exercise) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = exercises.size),
                colors = colors,
                icon = { SegmentedButtonDefaults.Icon(active = isActive) },
            ) {
                Text(exercise.label)
            }
        }
    }
}
