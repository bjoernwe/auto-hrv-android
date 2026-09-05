package dev.upaya.autohrv.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.R
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
fun MetricsRow(
    hr: Int?,
    hrv: String?,
    breathCycleSec: Float?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, outlineColor, shape)
                .background(surfaceColor)
                .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricCell(
            label = "HEART RATE",
            value = hr?.let { "$it" } ?: "—",
            unit = "bpm",
            trailingIcon = Icons.Filled.Favorite,
            iconTint = MaterialTheme.colorScheme.secondary,
            valueColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(38.dp)
                .background(outlineColor),
        )
        MetricCell(
            label = "BREATH",
            value = breathCycleSec?.let { "%.1f".format(it) } ?: "—",
            unit = "s",
            trailingIcon = ImageVector.vectorResource(R.drawable.ic_airwave),
            iconTint = MaterialTheme.colorScheme.primary,
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(38.dp)
                .background(outlineColor),
        )
        MetricCell(
            label = "HRV",
            value = hrv ?: "—",
            unit = "ms",
            trailingIcon = ImageVector.vectorResource(R.drawable.ic_balance),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(11.dp),
                )
            }
            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        letterSpacing = 0.1.em,
                        color = faint,
                    ),
            )
            if (trailingIcon != null) {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = valueColor,
                    ),
            )
            Text(
                text = unit,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = muted,
                    ),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun MetricsRowPreview() {
    AutoHrvTheme {
        MetricsRow(hr = 72, hrv = "42", breathCycleSec = 10.8f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "MetricsRow — no data")
@Composable
private fun MetricsRowNoDataPreview() {
    AutoHrvTheme {
        MetricsRow(hr = null, hrv = null, breathCycleSec = null)
    }
}
