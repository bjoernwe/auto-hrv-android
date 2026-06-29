package dev.upaya.autohrv.ui.hr

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun CouplingHeroCard(
    currentPhase: BreathingPhase,
    breathSamples: List<Sample>,
    rrSamples: List<Sample>,
    windowMs: Long,
    isInResonance: Boolean,
    modifier: Modifier = Modifier,
) {
    val nowMs by produceState(System.currentTimeMillis()) {
        while (true) { withFrameMillis { value = System.currentTimeMillis() } }
    }

    val lockStrength by animateFloatAsState(
        targetValue = if (isInResonance) 1f else 0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "lock-strength",
    )

    val breathColor = MaterialTheme.colorScheme.primary
    val heartColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val phaseLabel = if (currentPhase == BreathingPhase.Inhale) "Inhale" else "Exhale"

    // Target mean/range for the heart trace, updated when samples change.
    // We animate these to smooth out the jumps when new outliers enter/leave the window.
    val rrStats = remember(rrSamples) {
        val rrValues = rrSamples.map { it.value }
        if (rrValues.size >= 2) {
            val mean = rrValues.average().toFloat()
            val range = (rrValues.max() - rrValues.min()).coerceAtLeast(1f)
            mean to (range / 2f)
        } else {
            600f to 500f
        }
    }

    val animatedMean by animateFloatAsState(
        targetValue = rrStats.first,
        animationSpec = tween(5000, easing = LinearEasing),
        label = "rr-mean"
    )
    val animatedHalfRange by animateFloatAsState(
        targetValue = rrStats.second,
        animationSpec = tween(5000, easing = LinearEasing),
        label = "rr-half-range"
    )

    HrvCard(modifier = modifier) {
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

            // Both curves share one time-to-x mapping anchored at the same nowMs.
            fun xFor(t: Long) = padL + (1f - (nowMs - t).toFloat() / windowMs) * plotW

            // Subtle time grid (every 2 seconds)
            val windowSec = (windowMs / 1000).toInt()
            var gridSec = 0
            while (gridSec <= windowSec) {
                val gx = padL + (1f - gridSec.toFloat() / windowSec) * plotW
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(gx, padT),
                    end = Offset(gx, padT + plotH),
                    strokeWidth = 1.dp.toPx(),
                )
                gridSec += 2
            }

            // Breath trace
            val visibleBreath = breathSamples.filter { nowMs - it.tMillis <= windowMs }
            val breathPath = Path()
            if (visibleBreath.size >= 2) {
                visibleBreath.forEachIndexed { i, s ->
                    val x = xFor(s.tMillis)
                    val y = midY - (s.value * 2f - 1f) * breathAmp
                    if (i == 0) breathPath.moveTo(x, y) else breathPath.lineTo(x, y)
                }
            }
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

            // Heart (RR) trace — raw beats, one dot per heartbeat, same xFor(t) axis
            val visibleRr = rrSamples.filter { nowMs - it.tMillis <= windowMs }
            if (visibleRr.size >= 2) {
                // Invert RR: inhale → HR↑ → RR↓ → norm positive → trace rises with breath
                fun norm(v: Float) = -(v - animatedMean) / animatedHalfRange

                val heartPath = Path()
                val heartPoints = mutableListOf<Pair<Long, Offset>>()
                visibleRr.forEachIndexed { i, s ->
                    val x = xFor(s.tMillis)
                    val y = midY - norm(s.value) * heartAmp
                    if (i == 0) heartPath.moveTo(x, y) else heartPath.lineTo(x, y)
                    heartPoints.add(s.tMillis to Offset(x, y))
                }

                val heartBright = lerp(heartColor, Color.White, lockStrength * 0.35f)
                drawPath(
                    path = heartPath,
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
                for ((t, pt) in heartPoints) {
                    val ageFrac = (nowMs - t).toFloat() / windowMs
                    val alpha = (0.15f + 0.65f * (1f - ageFrac)).coerceIn(0f, 1f)
                    drawCircle(color = heartBright.copy(alpha = alpha), radius = 1.6.dp.toPx(), center = pt)
                }
            }

            // White bloom at lock
            if (lockStrength > 0.02f) {
                val bloomCenter = Offset(padL + plotW * 0.72f, midY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = lockStrength * 0.09f), Color.Transparent),
                        center = bloomCenter,
                        radius = plotW * 0.55f,
                    ),
                    radius = plotW * 0.55f,
                    center = bloomCenter,
                )
            }

            // Now-dot sits at the most recent breath sample and glides left with it
            val latestBreath = visibleBreath.lastOrNull()
            if (latestBreath != null) {
                val nowX = xFor(latestBreath.tMillis)
                val nowY = midY - (latestBreath.value * 2f - 1f) * breathAmp
                drawCircle(color = breathColor.copy(alpha = 0.15f), radius = 10.dp.toPx(), center = Offset(nowX, nowY))
                drawCircle(color = surfaceColor, radius = 4.2.dp.toPx(), center = Offset(nowX, nowY))
                drawCircle(color = breathColor, radius = 3.dp.toPx(), center = Offset(nowX, nowY))
            }
        }

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

private fun previewBreathSamples(sampleRateHz: Int = 4, windowMs: Long = 22_000L): List<Sample> {
    val now = System.currentTimeMillis()
    val count = sampleRateHz * (windowMs / 1000).toInt()
    val intervalMs = 1000L / sampleRateHz
    val cycleLengthMs = 10_800L
    return List(count) { i ->
        val t = now - (count - 1 - i) * intervalMs
        val pos = (t % cycleLengthMs).toFloat() / cycleLengthMs
        Sample(t, (0.5f - 0.5f * cos(PI.toFloat() * 2f * pos)).coerceIn(0f, 1f))
    }
}

private fun previewRrSamples(windowMs: Long = 22_000L): List<Sample> {
    val now = System.currentTimeMillis()
    val beatIntervalMs = 850L
    val count = (windowMs / beatIntervalMs).toInt()
    return List(count) { i ->
        val t = now - (count - 1 - i) * beatIntervalMs
        Sample(t, 920f + sin(i * 0.8).toFloat() * 80f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Coupling hero — tuning")
@Composable
private fun CouplingHeroTuningPreview() {
    AutoHrvTheme {
        CouplingHeroCard(
            currentPhase = BreathingPhase.Inhale,
            breathSamples = previewBreathSamples(),
            rrSamples = previewRrSamples(),
            windowMs = 22_000L,
            isInResonance = false,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "Coupling hero — locked")
@Composable
private fun CouplingHeroLockedPreview() {
    AutoHrvTheme {
        CouplingHeroCard(
            currentPhase = BreathingPhase.Exhale,
            breathSamples = previewBreathSamples(),
            rrSamples = previewRrSamples(),
            windowMs = 22_000L,
            isInResonance = true,
        )
    }
}
