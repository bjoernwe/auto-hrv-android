package com.polar.polarsdkecghrdemo.ui.hr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.polar.polarsdkecghrdemo.ui.breathing.BreathingPacerViewModel
import com.polar.polarsdkecghrdemo.ui.breathing.BreathingSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HRScreen(hrViewModel: PolarViewModel, breathingViewModel: BreathingPacerViewModel) {
    val uiState by hrViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Auto HRV") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            DeviceInfoSection(
                deviceId = hrViewModel.deviceId,
                connectionState = uiState.connectionState,
                batteryLevel = uiState.batteryLevel,
                firmwareVersion = uiState.firmwareVersion,
            )

            Spacer(Modifier.height(32.dp))

            HrSection(hr = uiState.hr, rrMs = uiState.rrMs)

            if (uiState.hrHistory.size >= 2) {
                Spacer(Modifier.height(16.dp))
                HrChart(
                    hrHistory = uiState.hrHistory,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 28.dp))

            Text(
                "Breathing Pacer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            BreathingSection(viewModel = breathingViewModel)

            Spacer(Modifier.height(24.dp))
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
            Spacer(Modifier.height(8.dp))
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
