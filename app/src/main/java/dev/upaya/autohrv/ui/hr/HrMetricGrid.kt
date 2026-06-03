package dev.upaya.autohrv.ui.hr

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun MetricsRow(hr: Int?, hrv: String?, rr: Int?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
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
            showHeart = true,
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
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(38.dp)
                .background(outlineColor),
        )
        MetricCell(
            label = "INTERVAL",
            value = rr?.let { "$it" } ?: "—",
            unit = "ms",
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
    showHeart: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
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
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.1.em,
                    color = faint,
                ),
            )
            if (showHeart) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = accent,
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
                style = TextStyle(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
                text = unit,
                style = TextStyle(
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = muted,
                ),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}
