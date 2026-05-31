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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.polarsdkecghrdemo.ui.breathing.BreathingPacerViewModel
import com.polar.polarsdkecghrdemo.ui.breathing.BreathingSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HRScreen(hrViewModel: PolarViewModel, breathingViewModel: BreathingPacerViewModel) {
    val uiState by hrViewModel.uiState.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

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
            )

            Spacer(Modifier.height(24.dp))

            HrMetricGrid(
                metrics = listOf(
                    HrMetric("Heart Rate (bpm)", uiState.hr?.let { "$it" } ?: "—"),
                    HrMetric("Variance", uiState.stats?.variance?.let { "%.0f".format(it) } ?: "—"),
                    HrMetric("Smoothness", uiState.stats?.smoothness?.let { "%.2f".format(it) } ?: "—"),
                    HrMetric("Periodicity", uiState.stats?.periodicity?.let { "%.2f".format(it) } ?: "—"),
                    HrMetric("Peak Power", uiState.stats?.peakPower?.let { "%.0fk".format(it.div(1000)) } ?: "—"),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.rrsMsHistory.size >= 2) {
                Spacer(Modifier.height(16.dp))
                TimeSeriesChart(
                    ts = uiState.rrsMsHistory,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val powerSpectrum = uiState.stats?.powerSpectrum
            if (powerSpectrum != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Power Spectrum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                PowerSpectrumChart(
                    spectrum = powerSpectrum,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                "Breathing Pacer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            BreathingSection(viewModel = breathingViewModel)

            if (uiState.experimentHistory.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(Modifier.padding(vertical = 28.dp))
                Text(
                    "Experiment History",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "x: Cycle length  ·  y: Out:In ratio  ·  opacity: periodicity  ·  ⊕: sampling mean",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                ExperimentScatterPlot(
                    history = uiState.experimentHistory,
                    samplingMean = uiState.samplingMean,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val samplingMean = uiState.samplingMean
            if (samplingMean != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(Modifier.padding(vertical = 28.dp))
                Text(
                    "Current Estimate",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                HrMetricGrid(
                    metrics = listOf(
                        HrMetric("Out:In Ratio", "%.1f".format(samplingMean.outToInRatio)),
                        HrMetric("Cycle Length (s)", "%.1f".format(samplingMean.cycleLengthSeconds)),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoSection(
    deviceId: String,
    connectionState: ConnectionState,
    batteryLevel: Int?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Device ID: $deviceId",
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
    }
}

private fun ConnectionState.label(): String = when (this) {
    is ConnectionState.Idle -> "Not connected"
    is ConnectionState.Connecting -> "Connecting…"
    is ConnectionState.Connected -> "Connected"
    is ConnectionState.Disconnected -> "Disconnected"
}

