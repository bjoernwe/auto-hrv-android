package dev.upaya.autohrv.ui.acf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

// Wide enough for "1 +1.8", so the row keeps its width when the scores come and go.
private val PILL_MIN_WIDTH = 52.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * The [count] principal-component toggles that sit in the ACF card's header.
 *
 * With nothing selected each pill also reads out that component's standardized score for the
 * current curve — how many of its own standard deviations the curve sits along it — so the numbers
 * stay comparable across components of very different magnitude. Selecting a pill switches the
 * chart's bars to that component's loadings; tapping it again returns to the accumulated histogram.
 */
@Composable
internal fun PrincipalComponentButtons(
    selected: Int?,
    standardizedScores: List<Float>,
    count: Int,
    enabled: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val score = standardizedScores.getOrNull(index)
            PrincipalComponentPill(
                label =
                    if (selected == null && score != null) {
                        "${index + 1} ${"%+.1f".format(score)}"
                    } else {
                        "${index + 1}"
                    },
                active = selected == index,
                enabled = enabled,
                onClick = { onSelect(if (selected == index) null else index) },
            )
        }
    }
}

@Composable
private fun PrincipalComponentPill(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    // Same breath-side identity as ExerciseButtonGroup and the band slider: these all say
    // "this is what the chart is showing you".
    val breath = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(50)
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val container = if (active) breath.copy(alpha = 0.12f * alpha) else MaterialTheme.colorScheme.surface
    val border = if (active) breath.copy(alpha = 0.22f * alpha) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
    val content = (if (active) breath else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .widthIn(min = PILL_MIN_WIDTH)
                .background(container, shape)
                .border(1.dp, border, shape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = content),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun PrincipalComponentButtonsPreview() {
    AutoHrvTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrincipalComponentButtons(
                selected = null,
                standardizedScores = listOf(1.8f, -0.4f, 0.2f),
                count = 3,
                enabled = true,
                onSelect = {},
            )
            PrincipalComponentButtons(
                selected = 1,
                standardizedScores = listOf(1.8f, -0.4f, 0.2f),
                count = 3,
                enabled = true,
                onSelect = {},
            )
        }
    }
}
