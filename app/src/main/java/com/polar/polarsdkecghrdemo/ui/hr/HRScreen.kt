package com.polar.polarsdkecghrdemo.ui.hr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.polarsdkecghrdemo.data.model.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HRScreen(viewModel: PolarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("HR / RR") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DeviceInfoSection(
                deviceId = viewModel.deviceId,
                connectionState = uiState.connectionState,
                batteryLevel = uiState.batteryLevel,
                firmwareVersion = uiState.firmwareVersion,
            )

            Spacer(modifier = Modifier.height(48.dp))

            HrSection(
                hr = uiState.hr,
                rrMs = uiState.rrMs,
            )
        }
    }
}

@Composable
private fun DeviceInfoSection(
    deviceId: String,
    connectionState: ConnectionState,
    batteryLevel: Int?,
    firmwareVersion: String?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "ID: $deviceId",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = connectionState.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (batteryLevel != null) {
            Text(
                text = "Battery: $batteryLevel%",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (firmwareVersion != null) {
            Text(
                text = "Firmware: $firmwareVersion",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HrSection(hr: Int?, rrMs: List<Int>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = hr?.toString() ?: "—",
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC00000),
        )
        if (rrMs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "(${rrMs.joinToString(separator = "ms, ")}ms)",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF0099CC),
            )
        }
    }
}

private fun ConnectionState.label(): String = when (this) {
    is ConnectionState.Idle -> "Not connected"
    is ConnectionState.Connecting -> "Connecting…"
    is ConnectionState.Connected -> "Connected"
    is ConnectionState.Disconnected -> "Disconnected"
}
