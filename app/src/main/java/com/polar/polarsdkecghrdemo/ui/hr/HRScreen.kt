package com.polar.polarsdkecghrdemo.ui.hr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.polarsdkecghrdemo.domain.breathing.ExperimentRecord
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
                    HrMetric("Smoothness", uiState.stats?.smoothness?.let { "%.2f".format(it) } ?: "—"),
                    HrMetric("Periodicity", uiState.stats?.periodicity?.let { "%.2f".format(it) } ?: "—"),
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
                Spacer(Modifier.height(16.dp))
                Text(
                    "Experiment History",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "x: Out:In ratio  ·  y: Cycle length  ·  opacity: periodicity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                ExperimentScatterPlot(
                    history = uiState.experimentHistory,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                ExperimentRecordTable(
                    records = uiState.experimentHistory,
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

@Composable
private fun ExperimentRecordTable(records: List<ExperimentRecord>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ExperimentRecordRow(
            index = "#",
            outToIn = "Out:In",
            cycle = "Cycle",
            periodicity = "Periodicity",
            header = true,
        )
        HorizontalDivider()
        records.reversed().forEachIndexed { index, record ->
            ExperimentRecordRow(
                index = "${index + 1}",
                outToIn = "%.1f".format(record.params.outToInRatio),
                cycle = "%.1f s".format(record.params.cycleLengthSeconds),
                periodicity = "%.2f".format(record.periodicity),
            )
        }
    }
}

@Composable
private fun ExperimentRecordRow(
    index: String,
    outToIn: String,
    cycle: String,
    periodicity: String,
    header: Boolean = false,
) {
    val style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val weight = if (header) FontWeight.SemiBold else FontWeight.Normal
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(index,     modifier = Modifier.weight(0.5f), style = style, fontWeight = weight, color = color, textAlign = TextAlign.End)
        Text(outToIn,   modifier = Modifier.weight(1f),   style = style, fontWeight = weight, color = color, textAlign = TextAlign.End)
        Text(cycle,     modifier = Modifier.weight(1f),   style = style, fontWeight = weight, color = color, textAlign = TextAlign.End)
        Text(periodicity, modifier = Modifier.weight(1f), style = style, fontWeight = weight, color = color, textAlign = TextAlign.End)
    }
}
