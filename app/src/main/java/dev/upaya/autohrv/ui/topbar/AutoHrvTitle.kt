package dev.upaya.autohrv.ui.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

/** Left-hand app title (heart glyph + "Auto HRV"). Rendered as the pinned top-left overlay. */
@Composable
internal fun AutoHrvTitle() {
    val accent = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            "Auto HRV",
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface,
                    letterSpacing = (-0.01).em,
                ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Title")
@Composable
private fun AutoHrvTitlePreview() {
    AutoHrvTheme {
        AutoHrvTitle()
    }
}
