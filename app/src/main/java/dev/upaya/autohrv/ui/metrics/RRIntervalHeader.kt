package dev.upaya.autohrv.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.ui.commons.HrvCard
import dev.upaya.autohrv.ui.commons.SectionLabel
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
internal fun RRIntervalHeader(swing: Int?) {
    val accent = MaterialTheme.colorScheme.secondary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val faint = muted.copy(alpha = 0.6f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            SectionLabel(text = "R–R INTERVAL")
            Spacer(Modifier.height(2.dp))
            Text(
                text = "beat-to-beat, ms",
                style = MaterialTheme.typography.labelMedium.copy(color = muted),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = swing?.let { "$it" } ?: "—",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        ),
                )
                Text(
                    text = "ms",
                    style = MaterialTheme.typography.labelMedium.copy(color = muted),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = "SWING",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        letterSpacing = 0.1.em,
                        color = faint,
                    ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "RRIntervalHeader — swing")
@Composable
private fun RRIntervalHeaderPreview() {
    AutoHrvTheme {
        HrvCard { RRIntervalHeader(swing = 284) }
    }
}
