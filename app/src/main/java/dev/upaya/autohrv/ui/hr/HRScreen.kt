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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.upaya.autohrv.ui.hr.charts.AutoCorrelationChart
import dev.upaya.autohrv.ui.hr.charts.SpectrogramBandView
import dev.upaya.autohrv.ui.hr.charts.SpectrogramChart

@Composable
fun HRScreen(viewModel: HrvViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val breathSamples by viewModel.breathSamples.collectAsStateWithLifecycle()
    val rrSamples by viewModel.rrSamples.collectAsStateWithLifecycle()
    val targetCycleLengthRange by viewModel.targetCycleLengthRange.collectAsStateWithLifecycle()
    val spectrogramBandSlices by viewModel.spectrogramBandSlices.collectAsStateWithLifecycle()
    // Static band info only changes identity when the slices do, so rebuild the views then — not on
    // every unrelated recomposition (which streams several times a second).
    val spectrogramBands =
        remember(spectrogramBandSlices) {
            viewModel.spectrogramBands.mapIndexed { i, info ->
                SpectrogramBandView(
                    label = info.label,
                    slices = spectrogramBandSlices.getOrElse(i) { emptyList() },
                    freqBinsHz = info.freqBinsHz,
                )
            }
        }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val hrv = uiState.rmssd
    val cycleLengthSec = uiState.currentPattern.cycleLengthSeconds

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
            modifier =
                Modifier
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))

                MetricsRow(
                    hr = uiState.hr,
                    hrv = hrv?.let { "%.0f".format(it) },
                    breathCycleSec = cycleLengthSec.takeIf { it > 0f },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))

                val acf = uiState.autoCorrelation
                val acfReady = acf != null && acf.size >= 2
                HrvCard {
                    ACFHeader()
                    Spacer(Modifier.height(6.dp))
                    if (acfReady) {
                        AutoCorrelationChart(
                            acf = acf,
                            histogram = uiState.acfHistogram,
                            peakLag =
                                uiState.autoCorrelationPeak
                                    ?.coerceIn(
                                        targetCycleLengthRange.first.toFloat(),
                                        targetCycleLengthRange.last.toFloat(),
                                    ),
                            bandLo = targetCycleLengthRange.first.toFloat(),
                            bandHi = targetCycleLengthRange.last.toFloat(),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                        )
                        BandRangeSlider(
                            value = targetCycleLengthRange,
                            onValueChange = { viewModel.setTargetCycleLengthRange(it) },
                            valueRange = 0..(acf.size - 1),
                            allowedRange = viewModel.cycleLengthAllowedRange,
                        )
                    } else {
                        ChartPlaceholder(
                            elapsedSeconds = uiState.acfHistorySeconds,
                            windowSeconds = viewModel.acfWindowSeconds,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                ExerciseButtonGroup(
                    activeRange = targetCycleLengthRange,
                    onSelect = { exercise -> viewModel.setTargetCycleLengthRange(exercise.cycleLengthRange) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))

                HrvCard {
                    SpectrogramHeader()
                    Spacer(Modifier.height(6.dp))
                    if (spectrogramBands.any { it.slices.isNotEmpty() }) {
                        SpectrogramChart(
                            bands = spectrogramBands,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(190.dp),
                        )
                    } else {
                        ChartPlaceholder(
                            elapsedSeconds = uiState.spectrogramHistorySeconds,
                            windowSeconds = viewModel.spectrogramWindowSeconds,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                /*InOutBiasCard(
                    bias = targetInOutBias,
                    onBiasChange = { viewModel.setTargetInOutBias(it) },
                    modifier = Modifier.fillMaxWidth(),
                )*/
            } // end padded column
        }
    }
}
