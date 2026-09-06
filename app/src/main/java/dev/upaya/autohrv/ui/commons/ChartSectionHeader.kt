package dev.upaya.autohrv.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
internal fun ChartSectionHeader(
    label: String,
    subtitle: String,
) = ChartSectionHeader(label = label) {
    Text(
        text = subtitle,
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

/** [ChartSectionHeader] with an arbitrary [trailing] element in place of the subtitle text. */
@Composable
internal fun ChartSectionHeader(
    label: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = label)
        trailing()
    }
}
