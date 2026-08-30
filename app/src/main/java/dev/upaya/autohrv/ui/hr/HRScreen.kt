package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.upaya.autohrv.ui.hr.charts.AutoCorrelationChart
import dev.upaya.autohrv.ui.hr.charts.SpectrogramBandView
import dev.upaya.autohrv.ui.hr.charts.SpectrogramChart

// Top padding of the pinned title row / scrolling connection-chip row, below the status bar.
private val TOP_BAR_TOP_PADDING = 10.dp

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
    val latestBreathValue = breathSamples.lastOrNull()?.value ?: 0f

    val scrollState = rememberScrollState()
    // Root-space Y of the pinned title row (where the breathing chip comes to rest when pinned) and
    // the resting Y of the chip's in-hero placeholder at scroll == 0. Both drive the sticky offset.
    var pinRootY by remember { mutableFloatStateOf(0f) }
    var restRootYAtZero by remember { mutableFloatStateOf(0f) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Manage window insets ourselves (statusBarsPadding on the top overlays / connection row,
        // navigationBarsPadding at the bottom) so the sticky chip can pin flush to the top edge.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Connection chip lives in the scroll flow (top-right, on the title's row) so it
                // scrolls off the top as the breathing chip rises to take its place.
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 18.dp, end = 18.dp, top = TOP_BAR_TOP_PADDING, bottom = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ConnectionChip(
                        deviceId = viewModel.deviceId,
                        connectionState = uiState.connectionState,
                        batteryLevel = uiState.batteryLevel,
                    )
                }

                CouplingHeroCard(
                    breathSamples = breathSamples,
                    rrSamples = rrSamples,
                    windowMs = viewModel.displayWindowMs,
                    isInResonance = uiState.isInResonance,
                    modifier = Modifier.fillMaxWidth(),
                    // Invisible placeholder: reserves the chip's resting slot in the hero header and
                    // reports its position so the real (overlay) chip knows where to rest.
                    phaseChipSlot = {
                        BreathingChip(
                            phase = uiState.currentPhaseStart.phase,
                            latestBreathValue = latestBreathValue,
                            modifier =
                                Modifier
                                    .alpha(0f)
                                    .onGloballyPositioned {
                                        restRootYAtZero = it.positionInRoot().y + scrollState.value
                                    },
                        )
                    },
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

                    Spacer(Modifier.height(16.dp))

                    ExerciseButtonGroup(
                        activeRange = targetCycleLengthRange,
                        onSelect = { exercise -> viewModel.setTargetCycleLengthRange(exercise.cycleLengthRange) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

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
                } // end padded column
            }

            // Pinned title (top-left) — stays put while everything scrolls beneath it.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 18.dp, top = TOP_BAR_TOP_PADDING)
                        .onGloballyPositioned { pinRootY = it.positionInRoot().y },
            ) {
                AutoHrvTitle()
            }

            // Sticky breathing chip (top-right). Shares the title's top padding, so translationY == 0
            // means "pinned on the title row." It tracks its in-hero placeholder until it reaches the
            // pin, then holds there.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = TOP_BAR_TOP_PADDING, end = 18.dp)
                        .graphicsLayer {
                            translationY = maxOf(0f, restRootYAtZero - pinRootY - scrollState.value)
                        },
            ) {
                BreathingChip(
                    phase = uiState.currentPhaseStart.phase,
                    latestBreathValue = latestBreathValue,
                )
            }
        }
    }
}
