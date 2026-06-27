package dev.upaya.autohrv.ui.hr

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.sdk.api.model.PolarDeviceInfo
import dev.upaya.autohrv.data.model.ConnectionState
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.domain.breathing.BreathingState
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.PI
import kotlin.math.cos

private const val COUPLING_WIN_SEC = 22f

@Composable
fun HRScreen(viewModel: HrvViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val breathingState by viewModel.breathingState.collectAsStateWithLifecycle()
    val currentPattern by viewModel.currentPattern.collectAsStateWithLifecycle()
    val targetCycleLengthRange by viewModel.targetCycleLengthRange.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val hrv = uiState.rmssd
    val currentRR = uiState.rrsMsHistory.lastOrNull()
    val cycleLengthSec = currentPattern.cycleLengthSeconds
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
                )
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ① Coupling hero — replaces the breathing orb
            CouplingHeroCard(
                breathingState = breathingState,
                pattern = currentPattern,
                rrsMsHistory = uiState.rrsMsHistory,
                isInResonance = uiState.isInResonance,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            ResonancePill(
                cycleLengthSec = cycleLengthSec,
                breathsPerMin = breathsPerMin,
                lagSeconds = uiState.lagSeconds,
                isInResonance = uiState.isInResonance,
            )

            Spacer(Modifier.height(12.dp))

            // ② R–R interval card
            HrvCard {
                RRIntervalHeader(swing = uiState.swing)
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

            // ④ Metrics row
            MetricsRow(
                hr = uiState.hr,
                hrv = hrv?.let { "%.0f".format(it) },
                rr = currentRR,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Coupling hero ────────────────────────────────────────────────────────────

@Composable
private fun CouplingHeroCard(
    breathingState: BreathingState,
    pattern: BreathingPattern,
    rrsMsHistory: List<Int>,
    isInResonance: Boolean,
    modifier: Modifier = Modifier,
) {
    val lockStrength by animateFloatAsState(
        targetValue = if (isInResonance) 1f else 0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "lock-strength",
    )

    // Capture colors before the Canvas lambda
    val breathColor = MaterialTheme.colorScheme.primary
    val heartColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // Reconstruct historical breath wave from current pacer position
    val T = pattern.cycleLengthSeconds.coerceAtLeast(1f)
    val inhaleSec = T / (1f + pattern.outToInRatio)
    val exhaleSec = (T - inhaleSec).coerceAtLeast(0.001f)
    val elapsedInPhase = breathingState.progress *
        (if (breathingState.phase == BreathingPhase.Inhale) inhaleSec else exhaleSec)
    val elapsedInCycle =
        if (breathingState.phase == BreathingPhase.Inhale) elapsedInPhase
        else inhaleSec + elapsedInPhase

    val phaseLabel = if (breathingState.phase == BreathingPhase.Inhale) "Inhale" else "Exhale"

    HrvCard(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel("COUPLING")
                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,
                    ),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            ResonanceChip(isInResonance = isInResonance)
        }

        Spacer(Modifier.height(10.dp))

        // Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val padL = 4.dp.toPx()
            val padR = 4.dp.toPx()
            val padT = 8.dp.toPx()
            val padB = 22.dp.toPx()
            val plotW = size.width - padL - padR
            val plotH = size.height - padT - padB
            val midY = padT + plotH / 2f
            val breathAmp = plotH * 0.36f
            val heartAmp = plotH * 0.34f

            // Subtle time grid
            var gridT = 0f
            while (gridT <= COUPLING_WIN_SEC) {
                val gx = padL + (1f - gridT / COUPLING_WIN_SEC) * plotW
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(gx, padT),
                    end = Offset(gx, padT + plotH),
                    strokeWidth = 1.dp.toPx(),
                )
                gridT += 2f
            }

            // Breath value at `secondsAgo` in the past; returns 0..1 (0=exhaled, 1=inhaled)
            fun breathAt(secondsAgo: Float): Float {
                val pos = ((elapsedInCycle - secondsAgo) % T + T) % T
                return if (pos < inhaleSec) {
                    val p = pos / inhaleSec
                    0.5f - 0.5f * cos(PI.toFloat() * p)
                } else {
                    val p = (pos - inhaleSec) / exhaleSec
                    0.5f + 0.5f * cos(PI.toFloat() * p)
                }
            }
            // Map to signed amplitude: +1 = inhale peak, -1 = exhale trough
            fun breathNorm(secondsAgo: Float) = breathAt(secondsAgo) * 2f - 1f

            // Breath path
            val steps = (plotW / 2).toInt().coerceAtLeast(4)
            val breathPath = Path()
            for (i in 0..steps) {
                val frac = i.toFloat() / steps
                val x = padL + frac * plotW
                val sAgo = COUPLING_WIN_SEC * (1f - frac)
                val y = midY - breathNorm(sAgo) * breathAmp
                if (i == 0) breathPath.moveTo(x, y) else breathPath.lineTo(x, y)
            }

            // Breath area fill
            val breathAreaPath = Path().apply {
                addPath(breathPath)
                lineTo(padL + plotW, padT + plotH)
                lineTo(padL, padT + plotH)
                close()
            }
            drawPath(
                path = breathAreaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(breathColor.copy(alpha = 0.18f), breathColor.copy(alpha = 0f)),
                    startY = midY - breathAmp,
                    endY = padT + plotH,
                ),
            )

            // Breath line
            val breathBright = lerp(breathColor, Color.White, lockStrength * 0.25f)
            drawPath(
                path = breathPath,
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to breathColor.copy(alpha = 0f),
                        0.10f to breathColor.copy(alpha = 0.80f),
                        1f to breathBright,
                    ),
                    startX = padL,
                    endX = padL + plotW,
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Heart (RR) trace
            // rrsMsHistory is 1 Hz zero-order-hold resampled: each element = 1 second.
            // index last = now (0 s ago), index last-k = k seconds ago.
            if (rrsMsHistory.size >= 2) {
                val rrMean = rrsMsHistory.average().toFloat()
                val rrRange = (rrsMsHistory.max() - rrsMsHistory.min()).toFloat().coerceAtLeast(1f)
                val halfRange = rrRange / 2f
                val n = rrsMsHistory.size

                fun hx(sAgo: Float) = padL + (1f - sAgo / COUPLING_WIN_SEC) * plotW
                fun hy(norm: Float) = midY - norm * heartAmp
                // Invert RR: inhale → HR↑ → RR↓ → norm positive → trace rises with breath
                fun norm(rr: Int) = -(rr - rrMean) / halfRange

                val heartBright = lerp(heartColor, Color.White, lockStrength * 0.35f)
                val ghostPath = Path()
                val heartPath = Path()
                var started = false

                rrsMsHistory.forEachIndexed { i, rr ->
                    val sAgo = (n - 1 - i).toFloat()   // 1 Hz: sample spacing = 1 s
                    if (sAgo > COUPLING_WIN_SEC) return@forEachIndexed
                    val x = hx(sAgo)
                    val y = hy(norm(rr))
                    if (!started) {
                        ghostPath.moveTo(x, y); heartPath.moveTo(x, y); started = true
                    } else {
                        ghostPath.lineTo(x, y); heartPath.lineTo(x, y)
                    }
                }

                if (started) {
                    drawPath(
                        path = ghostPath,
                        color = heartColor.copy(alpha = 0.20f),
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    drawPath(
                        path = heartPath,
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to heartColor.copy(alpha = 0f),
                                0.10f to heartColor.copy(alpha = 0.75f),
                                1f to heartBright,
                            ),
                            startX = padL,
                            endX = padL + plotW,
                        ),
                        style = Stroke(
                            width = (1.8f + lockStrength * 0.6f).dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                    rrsMsHistory.forEachIndexed { i, rr ->
                        val sAgo = (n - 1 - i).toFloat()
                        if (sAgo > COUPLING_WIN_SEC) return@forEachIndexed
                        val alpha = (0.15f + 0.65f * (1f - sAgo / COUPLING_WIN_SEC)).coerceIn(0f, 1f)
                        drawCircle(
                            color = heartBright.copy(alpha = alpha),
                            radius = 1.6.dp.toPx(),
                            center = Offset(hx(sAgo), hy(norm(rr))),
                        )
                    }
                }
            }

            // White bloom at lock
            if (lockStrength > 0.02f) {
                val bloomCenter = Offset(padL + plotW * 0.72f, midY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = lockStrength * 0.09f),
                            Color.Transparent,
                        ),
                        center = bloomCenter,
                        radius = plotW * 0.55f,
                    ),
                    radius = plotW * 0.55f,
                    center = bloomCenter,
                )
            }

            // Now-dot on breath wave
            val nowY = midY - breathNorm(0f) * breathAmp
            val nowX = padL + plotW
            drawCircle(
                color = breathColor.copy(alpha = 0.15f),
                radius = 10.dp.toPx(),
                center = Offset(nowX, nowY),
            )
            drawCircle(
                color = surfaceColor,
                radius = 4.2.dp.toPx(),
                center = Offset(nowX, nowY),
            )
            drawCircle(
                color = breathColor,
                radius = 3.dp.toPx(),
                center = Offset(nowX, nowY),
            )
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                "breath",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                ),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "heart",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                ),
            )
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun AutoHrvTopBar(
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

// ─── Resonance pill ───────────────────────────────────────────────────────────

@Composable
private fun ResonancePill(
    cycleLengthSec: Float,
    breathsPerMin: Float?,
    lagSeconds: Float?,
    isInResonance: Boolean,
) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val heart = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .background(surface)
            .padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillNumber(value = "%.1f".format(cycleLengthSec), unit = "s", onSurface = onSurface, muted = muted)

        PillDivider(outline)

        PillNumber(
            value = breathsPerMin?.let { "%.1f".format(it) } ?: "—",
            unit = "/min",
            onSurface = onSurface,
            muted = muted,
        )

        PillDivider(outline)

        // τ lag readout — shown in heart color; "—" until cross-correlation is implemented
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = lagSeconds?.let { "%.1f".format(it) } ?: "—",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = heart,
                ),
            )
            Text(
                text = "s lag",
                style = MaterialTheme.typography.labelMedium.copy(color = muted),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        Spacer(Modifier.width(11.dp))

        ResonanceChip(isInResonance = isInResonance)
    }
}

@Composable
private fun PillNumber(value: String, unit: String, onSurface: Color, muted: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface,
            ),
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium.copy(color = muted),
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

@Composable
private fun PillDivider(color: Color) {
    Spacer(Modifier.width(13.dp))
    Box(Modifier.width(1.dp).height(18.dp).background(color))
    Spacer(Modifier.width(13.dp))
}

// ─── Resonance chip & beacon ──────────────────────────────────────────────────

@Composable
private fun ResonanceChip(isInResonance: Boolean) {
    val accent = if (isInResonance) MaterialTheme.colorScheme.primary
                 else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (isInResonance) accent.copy(alpha = 0.10f)
                     else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(6.dp)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ResonanceBeacon(accent = accent, pulsing = isInResonance)
        Text(
            text = if (isInResonance) "LOCKED" else "TUNING",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = accent,
            ),
        )
    }
}

@Composable
private fun ResonanceBeacon(accent: Color, pulsing: Boolean) {
    val dotSize = 7.dp
    val beaconSize = 18.dp

    if (!pulsing) {
        Box(Modifier.size(beaconSize), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dotSize).background(accent, CircleShape))
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "resonance-beacon")
    val glowProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val dotScale by transition.animateFloat(
        initialValue = 0.82f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot",
    )
    val ringScale by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1200),
        ),
        label = "ring-scale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.65f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1200),
        ),
        label = "ring-alpha",
    )

    Box(Modifier.size(beaconSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(beaconSize)
                .scale(0.7f + glowProgress * 0.6f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.45f * (1f - glowProgress * 0.7f)),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(beaconSize)
                .scale(ringScale)
                .border(1.dp, accent.copy(alpha = ringAlpha), CircleShape),
        )
        Box(
            Modifier
                .size(beaconSize)
                .scale(dotScale * (dotSize.value / beaconSize.value))
                .background(accent, CircleShape),
        )
    }
}

// ─── Card shell & shared primitives ───────────────────────────────────────────

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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "Waiting for data …",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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

// ─── R–R card header ──────────────────────────────────────────────────────────

@Composable
private fun RRIntervalHeader(swing: Int?) {
    val accent = MaterialTheme.colorScheme.secondary
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
                    text = swing?.let { "$it" } ?: "—",
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
                text = "SWING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    letterSpacing = 0.1.em,
                    color = faint,
                ),
            )
        }
    }
}

// ─── ACF card header ──────────────────────────────────────────────────────────

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
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, color = muted),
        )
    }
}

// ─── Band slider ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BandRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    allowedRange: ClosedFloatingPointRange<Float>,
) {
    val accent = MaterialTheme.colorScheme.primary
    val safeValueRange = if (valueRange.start < valueRange.endInclusive) valueRange else 0f..1f
    val coercedValue =
        value.start.coerceIn(allowedRange).coerceIn(safeValueRange)..
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
                onValueChange(
                    new.start.coerceIn(allowedRange)..new.endInclusive.coerceIn(allowedRange)
                )
            },
            valueRange = safeValueRange,
            steps = intSteps,
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(top = 2.dp),
            colors = sliderColors,
            startThumb = { ThumbWithLabel(label = "%.0f".format(coercedValue.start), accent = accent) },
            endThumb = { ThumbWithLabel(label = "%.0f".format(coercedValue.endInclusive), accent = accent) },
            track = { rangeSliderState ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                ) {
                    SliderDefaults.Track(
                        rangeSliderState = rangeSliderState,
                        modifier = Modifier.height(2.dp),
                        colors = sliderColors,
                        drawTick = { _, _ -> },
                    )
                }
            },
        )
    }
}

@Composable
private fun ThumbWithLabel(label: String, accent: Color) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.wrapContentSize(unbounded = true),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = labelColor),
            modifier = Modifier.offset(y = (-22).dp),
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(12.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .shadow(2.dp, CircleShape)
                    .background(accent, CircleShape),
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Coupling hero — tuning")
@Composable
private fun CouplingHeroTuningPreview() {
    AutoHrvTheme {
        CouplingHeroCard(
            breathingState = BreathingState(BreathingPhase.Inhale, 0.6f),
            pattern = BreathingPattern(outToInRatio = 1.5f, cycleLengthSeconds = 10.8f),
            rrsMsHistory = (0 until 30).map { i -> (920 + (kotlin.math.sin(i * 0.8) * 80).toInt()) },
            isInResonance = false,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Coupling hero — locked")
@Composable
private fun CouplingHeroLockedPreview() {
    AutoHrvTheme {
        CouplingHeroCard(
            breathingState = BreathingState(BreathingPhase.Exhale, 0.3f),
            pattern = BreathingPattern(outToInRatio = 1.5f, cycleLengthSeconds = 10.8f),
            rrsMsHistory = (0 until 30).map { i -> (920 + (kotlin.math.sin(i * 0.8) * 80).toInt()) },
            isInResonance = true,
        )
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
        ResonancePill(
            cycleLengthSec = 10.8f,
            breathsPerMin = 5.6f,
            lagSeconds = 2.3f,
            isInResonance = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL)
@Composable
private fun ResonancePillNoLagPreview() {
    AutoHrvTheme {
        ResonancePill(
            cycleLengthSec = 10.0f,
            breathsPerMin = 6.0f,
            lagSeconds = null,
            isInResonance = false,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "RRIntervalHeader — swing")
@Composable
private fun RRIntervalHeaderPreview() {
    AutoHrvTheme {
        HrvCard { RRIntervalHeader(swing = 284) }
    }
}
