package dev.upaya.autohrv.ui.hr

import androidx.compose.animation.core.FastOutSlowInEasing
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
import dev.upaya.autohrv.domain.breathing.BreathingPattern
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.domain.breathing.BreathingState
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import kotlin.math.PI
import kotlin.math.cos

private const val COUPLING_WIN_SEC = 22f

@Composable
internal fun CouplingHeroCard(
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

    // Reconstruct historical breath wave from current pacer position
    val T = pattern.cycleLengthSeconds.coerceAtLeast(1f)
    val inhaleSec = T / (1f + pattern.outToInRatio)
    val exhaleSec = (T - inhaleSec).coerceAtLeast(0.001f)
    val elapsedInPhase = breathingState.progress *
        (if (breathingState.phase == BreathingPhase.Inhale) inhaleSec else exhaleSec)
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

            // Breath stroke + area fill
            val steps = (plotW / 2).toInt().coerceAtLeast(4)
            val breathPath = Path()
            for (i in 0..steps) {
                val frac = i.toFloat() / steps
                val x = padL + frac * plotW
                val sAgo = COUPLING_WIN_SEC * (1f - frac)
                val y = midY - breathNorm(sAgo) * breathAmp
                if (i == 0) breathPath.moveTo(x, y) else breathPath.lineTo(x, y)
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
                val heartPath = Path()
                // Collect visible points (sAgo, center) in one pass for reuse in dot drawing.
                val heartPoints = mutableListOf<Pair<Float, Offset>>()

                rrsMsHistory.forEachIndexed { i, rr ->
                    val sAgo = (n - 1 - i).toFloat()   // 1 Hz: sample spacing = 1 s
                    if (sAgo > COUPLING_WIN_SEC) return@forEachIndexed
                    val pt = Offset(hx(sAgo), hy(norm(rr)))
                    if (heartPoints.isEmpty()) heartPath.moveTo(pt.x, pt.y)
                    else heartPath.lineTo(pt.x, pt.y)
                    heartPoints.add(sAgo to pt)
                }

                if (heartPoints.isNotEmpty()) {
                    // Ghost pass: same geometry, flat low-alpha color
                    drawPath(
                        path = heartPath,
                        color = heartColor.copy(alpha = 0.20f),
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    // Bright pass: horizontal gradient fading in from left
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
                    for ((sAgo, pt) in heartPoints) {
                        val alpha = (0.15f + 0.65f * (1f - sAgo / COUPLING_WIN_SEC)).coerceIn(0f, 1f)
                        drawCircle(
                            color = heartBright.copy(alpha = alpha),
                            radius = 1.6.dp.toPx(),
                            center = pt,
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
