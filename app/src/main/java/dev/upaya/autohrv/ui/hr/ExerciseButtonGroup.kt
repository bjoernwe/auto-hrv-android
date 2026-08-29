package dev.upaya.autohrv.ui.hr

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.upaya.autohrv.domain.breathing.exercises.Exercise

@Composable
fun ExerciseButtonGroup(
    active: Exercise?,
    onSelect: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = Exercise.all
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        exercises.forEachIndexed { index, exercise ->
            val isActive = exercise == active
            SegmentedButton(
                selected = isActive,
                onClick = { if (!isActive) onSelect(exercise) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = exercises.size),
                icon = { SegmentedButtonDefaults.Icon(active = isActive) },
            ) {
                Text(exercise.label)
            }
        }
    }
}
