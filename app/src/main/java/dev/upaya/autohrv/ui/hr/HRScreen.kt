package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.upaya.autohrv.ui.hr.charts.AutoCorrelationChart

@Composable
fun HRScreen(viewModel: HrvViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val breathSamples by viewModel.breathSamples.collectAsStateWithLifecycle()
    val rrSamples by viewModel.rrSamples.collectAsStateWithLifecycle()
    val targetCycleLengthRange by viewModel.targetCycleLengthRange.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val hrv = uiState.rmssd
    val currentRR = uiState.currentRr
    val cycleLengthSec = uiState.currentPattern.cycleLengthSeconds
    val breathsPerMin = if (cycleLengthSec > 0f) 60f / cycleLengthSec else null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AutoHrvTopBar(
                deviceId = viewModel.deviceId,
                connectionState = uiState.connectionState,
                batteryLevel = uiState.batteryLevel,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CouplingHeroCard(
                currentPhase = uiState.currentPhaseStart.phase,
                breathSamples = breathSamples,
                rrSamples = rrSamples,
                windowMs = viewModel.displayWindowMs,
                isInResonance = uiState.isInResonance,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

            Spacer(Modifier.height(12.dp))

            ResonancePill(
                cycleLengthSec = cycleLengthSec,
                breathsPerMin = breathsPerMin,
                lagSeconds = uiState.lagSeconds,
                isInResonance = uiState.isInResonance,
            )

            Spacer(Modifier.height(12.dp))

            /*HrvCard {
                RRIntervalHeader(swing = uiState.swing)
                if (rrSamples.size >= 2) {
                    TimeSeriesChart(
                        samples = rrSamples,
                        windowMs = viewModel.displayWindowMs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    )
                } else {
                    ChartPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))*/

            val acf = uiState.autoCorrelation
            val acfReady = acf != null && acf.size >= 2
            HrvCard {
                ACFHeader()
                Spacer(Modifier.height(6.dp))
                if (acfReady) {
                    AutoCorrelationChart(
                        acf = acf,
                        peakLag = uiState.autoCorrelationPeak
                            ?.coerceIn(targetCycleLengthRange),
                        bandLo = targetCycleLengthRange.start,
                        bandHi = targetCycleLengthRange.endInclusive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    )
                    BandRangeSlider(
                        value = targetCycleLengthRange,
                        onValueChange = { viewModel.setTargetCycleLengthRange(it) },
                        valueRange = 0f..(acf.size - 1).toFloat(),
                        allowedRange = viewModel.cycleLengthAllowedRange,
                    )
                } else {
                    ChartPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            MetricsRow(
                hr = uiState.hr,
                hrv = hrv?.let { "%.0f".format(it) },
                rr = currentRR,
                modifier = Modifier.fillMaxWidth(),
            )
            } // end padded column
        }
    }
}
