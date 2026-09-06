package dev.upaya.autohrv.ui.acf

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** A target cycle-length preset paired with the caption its button shows. */
private data class ExercisePreset(
    val label: String,
    val cycleLengthRange: IntRange,
)

@Composable
fun ExerciseButtonGroup(
    activeRange: IntRange,
    allowedRange: IntRange,
    onSelect: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Captions are presentation, so the presets live here rather than in the domain layer; "Free"
    // is the full range the settings repository allows.
    val exercises =
        remember(allowedRange) {
            listOf(
                ExercisePreset("Fast", 4..5),
                ExercisePreset("Med.", 6..7),
                ExercisePreset("Slow", 9..12),
                ExercisePreset("Free", allowedRange),
            )
        }
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
                onClick = { if (!isActive) onSelect(exercise.cycleLengthRange) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = exercises.size),
                colors = colors,
                icon = { SegmentedButtonDefaults.Icon(active = isActive) },
            ) {
                Text(exercise.label)
            }
        }
    }
}
