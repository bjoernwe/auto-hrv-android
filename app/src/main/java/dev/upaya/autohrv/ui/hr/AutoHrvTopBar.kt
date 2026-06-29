package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import com.polar.sdk.api.model.PolarDeviceInfo
import dev.upaya.autohrv.data.model.ConnectionState
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
internal fun AutoHrvTopBar(
    deviceId: String,
    connectionState: ConnectionState,
    batteryLevel: Int?,
) {
    val accent = MaterialTheme.colorScheme.secondary
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val outlineStrong = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val faint = muted.copy(alpha = 0.6f)
    val isConnected = connectionState is ConnectionState.Connected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface,
                    letterSpacing = (-0.01).em,
                ),
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, outlineStrong, RoundedCornerShape(999.dp))
                .background(surface2)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (isConnected) accent else faint, CircleShape),
            )
            Text(
                text = deviceId.take(8),
                style = MaterialTheme.typography.labelLarge.copy(color = onSurface),
            )
            if (batteryLevel != null) {
                Text(
                    text = "$batteryLevel%",
                    style = MaterialTheme.typography.labelMedium.copy(color = muted),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "TopBar — connected")
@Composable
private fun AutoHrvTopBarConnectedPreview() {
    AutoHrvTheme {
        AutoHrvTopBar(
            deviceId = "E7A9AB27",
            connectionState = ConnectionState.Connected(
                PolarDeviceInfo("E7A9AB27", "AA:BB:CC:DD:EE:FF", -60, "Polar H10", true)
            ),
            batteryLevel = 82,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "TopBar — disconnected")
@Composable
private fun AutoHrvTopBarDisconnectedPreview() {
    AutoHrvTheme {
        AutoHrvTopBar(
            deviceId = "E7A9AB27",
            connectionState = ConnectionState.Disconnected("E7A9AB27"),
            batteryLevel = null,
        )
    }
}
