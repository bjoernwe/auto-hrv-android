package dev.upaya.autohrv.ui.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.sdk.api.model.PolarDeviceInfo
import dev.upaya.autohrv.data.model.ConnectionState
import dev.upaya.autohrv.ui.breathing.BreathingPacerOrb
import dev.upaya.autohrv.ui.breathing.BreathingPacerViewModel
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
fun HRScreen(hrViewModel: HrvViewModel, breathingViewModel: BreathingPacerViewModel) {
    val uiState by hrViewModel.uiState.collectAsStateWithLifecycle()
    val breathingState by breathingViewModel.breathingState.collectAsStateWithLifecycle()
    val currentPattern by breathingViewModel.currentPattern.collectAsStateWithLifecycle()
    val targetCycleLengthRange by breathingViewModel.targetCycleLengthRange.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val rmssd = uiState.rmssd
    val currentRR = uiState.rrsMsHistory.lastOrNull()
    val hrv = uiState.rmssd
    val cycleLengthSec = currentPattern.cycleLengthSeconds
    val breathsPerMin = if (cycleLengthSec > 0f) 60f / cycleLengthSec else null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AutoHrvTopBar(
                deviceId = hrViewModel.deviceId,
                connectionState = uiState.connectionState,
                batteryLevel = uiState.batteryLevel,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // ① Breathing pacer — hero
            BreathingPacerOrb(
                state = breathingState,
                modifier = Modifier.size(188.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Resonance readout pill
            ResonancePill(
                cycleLengthSec = cycleLengthSec,
                breathsPerMin = breathsPerMin,
            )

            Spacer(Modifier.height(12.dp))

            // ② R–R interval card
            HrvCard {
                RRIntervalHeader(rmssd)
                if (uiState.rrsMsHistory.size >= 2) {
                    TimeSeriesChart(
                        ts = uiState.rrsMsHistory,
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
            Spacer(Modifier.height(12.dp))

            // ③ Autocorrelation card
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
                        onValueChange = { breathingViewModel.setTargetCycleLengthRange(it) },
                        valueRange = 0f..(acf.size - 1).toFloat(),
                        allowedRange = breathingViewModel.cycleLengthAllowedRange,
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

            // ④ Metrics row
            MetricsRow(
                hr = uiState.hr,
                hrv = hrv?.let { "%.0f".format(it) },
                rr = currentRR,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AutoHrvTopBar(
    deviceId: String,
    connectionState: ConnectionState,
    batteryLevel: Int?,
) {
    val accent = MaterialTheme.colorScheme.primary
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
        // Left: icon box + title
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

        // Right: status chip
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
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = muted,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ResonancePill(cycleLengthSec: Float, breathsPerMin: Float?) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .background(surface)
            .padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cycle length
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "%.1f".format(cycleLengthSec),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface
                ),
            )
            Text(
                text = "s",
                style = MaterialTheme.typography.labelMedium.copy(color = muted),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        Spacer(Modifier.width(13.dp))
        Box(Modifier.width(1.dp).height(18.dp).background(outline))
        Spacer(Modifier.width(13.dp))

        // Breaths per minute
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = breathsPerMin?.let { "%.1f".format(it) } ?: "—",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface
                ),
            )
            Text(
                text = "/min",
                style = MaterialTheme.typography.labelMedium.copy(color = muted),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        Spacer(Modifier.width(11.dp))

        // "In resonance" chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = 0.10f))
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(7.dp).background(accent, CircleShape))
            Text(
                text = "In resonance",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                ),
            )
        }
    }
}

@Composable
private fun HrvCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun ChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Waiting for data …",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun RRIntervalHeader(rmssd: Float?) {
    val accent = MaterialTheme.colorScheme.primary
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
                    text = rmssd?.let { "%.0f".format(it) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall.copy(
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
                text = "RMSSD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    letterSpacing = 0.1.em,
                    color = faint,
                ),
            )
        }
    }
}

@Composable
private fun ACFHeader() {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = "AUTOCORRELATION")
        Text(
            text = "peak → pace",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                color = muted,
            ),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.14.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BandRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    allowedRange: ClosedFloatingPointRange<Float>,
) {
    val accent = MaterialTheme.colorScheme.primary

    // Safety: ensure valueRange is valid and coercedValue is within it to avoid crashes in RangeSliderState.
    val safeValueRange = if (valueRange.start < valueRange.endInclusive) valueRange else 0f..1f
    val coercedValue = value.start.coerceIn(allowedRange).coerceIn(safeValueRange)..
            value.endInclusive.coerceIn(allowedRange).coerceIn(safeValueRange)

    val sliderColors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent.copy(alpha = 0.38f),
        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
    )

    Column {
        val intSteps = maxOf(0, (safeValueRange.endInclusive - safeValueRange.start).toInt() - 1)

        RangeSlider(
            value = coercedValue,
            onValueChange = { new ->
                onValueChange(new.start.coerceIn(allowedRange)..new.endInclusive.coerceIn(allowedRange))
            },
            valueRange = safeValueRange,
            steps = intSteps,
            modifier = Modifier.fillMaxWidth()
                .height(16.dp)
                .padding(top = 2.dp),
            colors = sliderColors,
            startThumb = {
                ThumbWithLabel(
                    label = "%.0f".format(coercedValue.start),
                    accent = accent
                )
            },
            endThumb = {
                ThumbWithLabel(
                    label = "%.0f".format(coercedValue.endInclusive),
                    accent = accent
                )
            },
            track = { rangeSliderState ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {
                    SliderDefaults.Track(
                        rangeSliderState = rangeSliderState,
                        modifier = Modifier.height(2.dp),
                        colors = sliderColors,
                        drawTick = { _, _ -> },
                    )
                }
            }
        )
    }
}

@Composable
private fun ThumbWithLabel(
    label: String,
    accent: Color,
    labelColor: Color  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.wrapContentSize(unbounded = true)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = labelColor,

            ),
            modifier = Modifier.offset(y = (-22).dp)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .shadow(2.dp, CircleShape)
                    .background(accent, CircleShape)
            )
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

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun ResonancePillPreview() {
    AutoHrvTheme {
        ResonancePill(cycleLengthSec = 10.0f, breathsPerMin = 6.0f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun HrvCardPreview() {
    AutoHrvTheme {
        HrvCard {
            RRIntervalHeader(rmssd = 42.3f)
            ChartPlaceholder(modifier = Modifier.fillMaxWidth().height(100.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun ChartPlaceholderPreview() {
    AutoHrvTheme {
        ChartPlaceholder(modifier = Modifier.fillMaxWidth().height(100.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "RRIntervalHeader — with data")
@Composable
private fun RRIntervalHeaderWithDataPreview() {
    AutoHrvTheme {
        RRIntervalHeader(rmssd = 42.3f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "RRIntervalHeader — no data")
@Composable
private fun RRIntervalHeaderNoDataPreview() {
    AutoHrvTheme {
        RRIntervalHeader(rmssd = null)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun ACFHeaderPreview() {
    AutoHrvTheme {
        ACFHeader()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun SectionLabelPreview() {
    AutoHrvTheme {
        SectionLabel(text = "R–R INTERVAL")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun BandRangeSliderPreview() {
    AutoHrvTheme {
        BandRangeSlider(
            value = 7f..13f,
            onValueChange = {},
            valueRange = 0f..60f,
            allowedRange = 4f..16f,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun ThumbWithLabelPreview() {
    AutoHrvTheme {
        ThumbWithLabel(label = "10", accent = MaterialTheme.colorScheme.primary)
    }
}
