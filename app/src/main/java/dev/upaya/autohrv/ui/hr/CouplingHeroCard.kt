package dev.upaya.autohrv.ui.hr

import android.graphics.RuntimeShader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.domain.breathing.BreathingPhase
import dev.upaya.autohrv.ui.hr.charts.smoothPath
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
        while (true) {
            withFrameMillis { value = System.currentTimeMillis() }
        }
    }

    val grainBrush = rememberGrainBrush()

    val lockStrength by animateFloatAsState(
        targetValue = if (isInResonance) 1f else 0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "lock-strength",
    )

    // Target mean/range for the heart trace, updated when samples change.
    // We animate these to smooth out the jumps when new outliers enter/leave the window.
    val rrStats = remember(rrSamples) { rrStats(rrSamples) }
    val animatedMean by animateFloatAsState(
        targetValue = rrStats.mean,
        animationSpec = tween(5000, easing = LinearEasing),
        label = "rr-mean",
    )
    val animatedHalfRange by animateFloatAsState(
        targetValue = rrStats.halfRange,
        animationSpec = tween(5000, easing = LinearEasing),
        label = "rr-half-range",
    )

    val breathColor = MaterialTheme.colorScheme.primary
    val heartColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background

    Column(modifier = modifier.fillMaxWidth()) {
        CouplingHeader(
            currentPhase = currentPhase,
            latestBreathValue = breathSamples.lastOrNull()?.value ?: 0f,
            breathColor = breathColor,
        )

        Spacer(Modifier.height(10.dp))

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
        ) {
            val geom =
                ChartGeometry(
                    size = size,
                    padTop = 8.dp.toPx(),
                    padBottom = 22.dp.toPx(),
                    dotGap = 5.dp.toPx(),
                    nowMs = nowMs,
                    windowMs = windowMs,
                )

            drawTimeGrid(geom)

            val visibleBreath = breathSamples.filter { nowMs - it.tMillis <= windowMs }
            val breathPoints =
                visibleBreath.map { s ->
                    Offset(geom.xFor(s.tMillis), geom.midY - (s.value * 2f - 1f) * geom.breathAmp)
                }
            // Stroke stops dotGap short of the edge so the now-dot's glow stays on-screen.
            val strokePoints = trimAtX(breathPoints, cutX = size.width - geom.dotGap)

            drawBreathTrace(geom, breathPoints, strokePoints, breathColor, lockStrength, grainBrush)
            drawHeartTrace(geom, rrSamples, heartColor, lockStrength) { v ->
                // Invert RR: inhale → HR↑ → RR↓ → norm positive → trace rises with breath
                -(v - animatedMean) / animatedHalfRange
            }
            drawLockBloom(geom, lockStrength)
            drawLeftEdgeFade(geom, backgroundColor)
            drawNowDot(strokePoints.lastOrNull(), visibleBreath.lastOrNull(), breathColor, backgroundColor)
        }
    }
}

@Composable
private fun CouplingHeader(
    currentPhase: BreathingPhase,
    latestBreathValue: Float,
    breathColor: Color,
) {
    // Drive label color from the live breath value (0=exhale, 1=inhale) so it brightens
    // toward teal as the user inhales and dims as they exhale — temporal anchor for the now-dot.
    val animatedBreathValue by animateFloatAsState(
        targetValue = latestBreathValue,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "breath-value",
    )
    val onSurface = MaterialTheme.colorScheme.onSurface
    val phaseLabelColor = lerp(onSurface.copy(alpha = 0.2f), breathColor, animatedBreathValue)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = if (currentPhase == BreathingPhase.Inhale) "inhale" else "exhale",
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = phaseLabelColor,
                ),
        )
    }
}

// --- Geometry & math -------------------------------------------------------

/** Shared plot geometry: paddings, amplitudes, and the time-to-x mapping anchored at nowMs. */
private class ChartGeometry(
    val size: Size,
    val padTop: Float,
    val padBottom: Float,
    val dotGap: Float,
    val nowMs: Long,
    val windowMs: Long,
) {
    val plotW = size.width
    val plotH = size.height - padTop - padBottom
    val midY = padTop + plotH / 2f
    val breathAmp = plotH * 0.36f
    val heartAmp = plotH * 0.34f

    /** Both curves share this one time-to-x mapping so they stay aligned. */
    fun xFor(t: Long) = (1f - (nowMs - t).toFloat() / windowMs) * plotW
}

private data class RrStats(
    val mean: Float,
    val halfRange: Float,
)

private fun rrStats(rrSamples: List<Sample>): RrStats {
    val rrValues = rrSamples.map { it.value }
    return if (rrValues.size >= 2) {
        val mean = rrValues.average().toFloat()
        val range = (rrValues.max() - rrValues.min()).coerceAtLeast(1f)
        RrStats(mean, range / 2f)
    } else {
        RrStats(600f, 500f)
    }
}

/**
 * Truncates a polyline at [cutX]: keeps points left of the cut and interpolates the exact
 * crossing so the stroke — and the now-dot riding on its endpoint — end at the same spot.
 */
private fun trimAtX(
    points: List<Offset>,
    cutX: Float,
): List<Offset> =
    buildList {
        for (i in points.indices) {
            val p = points[i]
            if (p.x <= cutX) {
                add(p)
            } else {
                val prev = points.getOrNull(i - 1)
                if (prev != null && prev.x < cutX) {
                    val frac = (cutX - prev.x) / (p.x - prev.x)
                    add(Offset(cutX, prev.y + (p.y - prev.y) * frac))
                }
                break
            }
        }
    }

private fun polylinePath(points: List<Offset>) =
    Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    }

// --- Drawing stages ---------------------------------------------------------

/** Subtle vertical time grid, one line every 2 seconds. */
private fun DrawScope.drawTimeGrid(geom: ChartGeometry) {
    val windowSec = (geom.windowMs / 1000).toInt()
    for (gridSec in 0..windowSec step 2) {
        val gx = (1f - gridSec.toFloat() / windowSec) * geom.plotW
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(gx, geom.padTop),
            end = Offset(gx, geom.padTop + geom.plotH),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun DrawScope.drawBreathTrace(
    geom: ChartGeometry,
    breathPoints: List<Offset>,
    strokePoints: List<Offset>,
    breathColor: Color,
    lockStrength: Float,
    grainBrush: Brush,
) {
    // Area fill uses the untruncated points and is held flat out to the true right edge,
    // so the gradient never leaves a black gap even where the stroke has pulled back.
    val breathAreaPath =
        Path().apply {
            if (breathPoints.size <= 2) {
                return@apply
            }
            val firstX = breathPoints.first().x
            breathPoints.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            lineTo(size.width, breathPoints.last().y)
            lineTo(size.width, geom.padTop + geom.plotH)
            lineTo(firstX, geom.padTop + geom.plotH)
            close()
        }
    drawPath(
        path = breathAreaPath,
        brush =
            Brush.verticalGradient(
                colors = listOf(breathColor.copy(alpha = 0.18f), breathColor.copy(alpha = 0f)),
                startY = geom.midY - geom.breathAmp,
                endY = geom.padTop + geom.plotH,
            ),
    )
    clipPath(breathAreaPath) {
        drawRect(brush = grainBrush, blendMode = BlendMode.Overlay)
    }
    val breathBright = lerp(breathColor, Color.White, lockStrength * 0.25f)
    drawPath(
        path = polylinePath(strokePoints),
        color = breathBright,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Heart (RR) trace — raw beats, one dot per heartbeat, same xFor(t) axis as the breath. */
private fun DrawScope.drawHeartTrace(
    geom: ChartGeometry,
    rrSamples: List<Sample>,
    heartColor: Color,
    lockStrength: Float,
    norm: (Float) -> Float,
) {
    // Keep a point if it or its successor is on-screen so no segment is dropped prematurely.
    // Time-based heuristics fail for irregular gaps, so we check x directly.
    val visibleRr =
        rrSamples.filterIndexed { i, s ->
            geom.xFor(s.tMillis) >= 0f || (i + 1 < rrSamples.size && geom.xFor(rrSamples[i + 1].tMillis) >= 0f)
        }
    if (visibleRr.size < 2) return

    val heartPoints =
        visibleRr.map { s ->
            s.tMillis to Offset(geom.xFor(s.tMillis), geom.midY - norm(s.value) * geom.heartAmp)
        }
    val heartPath = smoothPath(heartPoints.map { it.second })

    val heartBright = lerp(heartColor, Color.White, lockStrength * 0.35f)
    drawPath(
        path = heartPath,
        color = heartColor.copy(alpha = 0.20f),
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = heartPath,
        color = heartBright,
        style =
            Stroke(
                width = (1.8f + lockStrength * 0.6f).dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
    )
    for ((t, pt) in heartPoints) {
        val ageFrac = (geom.nowMs - t).toFloat() / geom.windowMs
        val alpha = (0.15f + 0.65f * (1f - ageFrac)).coerceIn(0f, 1f)
        drawCircle(color = heartBright.copy(alpha = alpha), radius = 1.6.dp.toPx(), center = pt)
    }
}

/** White bloom at lock. */
private fun DrawScope.drawLockBloom(
    geom: ChartGeometry,
    lockStrength: Float,
) {
    if (lockStrength <= 0.02f) return
    val bloomCenter = Offset(geom.plotW * 0.72f, geom.midY)
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = lockStrength * 0.09f), Color.Transparent),
                center = bloomCenter,
                radius = geom.plotW * 0.55f,
            ),
        radius = geom.plotW * 0.55f,
        center = bloomCenter,
    )
}

/** Left-edge fade overlay — masks entering curve segments cleanly regardless of gap size. */
private fun DrawScope.drawLeftEdgeFade(
    geom: ChartGeometry,
    backgroundColor: Color,
) {
    val fadeWidth = geom.plotW * 0.2f
    drawRect(
        brush =
            Brush.horizontalGradient(
                colors = listOf(backgroundColor, Color.Transparent),
                startX = 0f,
                endX = fadeWidth,
            ),
        topLeft = Offset(0f, geom.padTop),
        size = Size(fadeWidth, geom.plotH),
    )
}

/**
 * Now-dot: size breathes with the signal — larger on inhale, smaller on exhale.
 * Sits at the same trimmed endpoint as the stroke, so it never overhangs the line.
 */
private fun DrawScope.drawNowDot(
    nowPoint: Offset?,
    latestBreath: Sample?,
    breathColor: Color,
    backgroundColor: Color,
) {
    if (nowPoint == null || latestBreath == null) return
    val v = latestBreath.value
    val coreR = (2f + v * 2.5f).dp.toPx()
    val ringR = (3.5f + v * 3f).dp.toPx()
    val glowR = (8f + v * 6f).dp.toPx()
    drawCircle(color = breathColor.copy(alpha = 0.12f + v * 0.10f), radius = glowR, center = nowPoint)
    drawCircle(color = backgroundColor, radius = ringR, center = nowPoint)
    drawCircle(color = breathColor, radius = coreR, center = nowPoint)
}

// --- Grain shader -----------------------------------------------------------

// AGSL grain: per-pixel hash noise. "time" is a fixed seed (set once, not per-frame) so the
// speckle pattern stays put instead of flickering.
private const val GRAIN_SHADER_SRC = """
    uniform float time;
    uniform float intensity;

    float hash(float2 p) {
        p = fract(p * float2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    half4 main(float2 fragCoord) {
        float n = hash(fragCoord + time);
        return half4(n, n, n, intensity);
    }
"""

@Composable
private fun rememberGrainBrush(intensity: Float = 0.3f): Brush =
    remember(intensity) {
        ShaderBrush(
            RuntimeShader(GRAIN_SHADER_SRC).apply {
                setFloatUniform("time", 0f)
                setFloatUniform("intensity", intensity)
            },
        )
    }

// --- Previews ----------------------------------------------------------------

private fun previewBreathSamples(
    sampleRateHz: Int = 4,
    windowMs: Long = 22_000L,
): List<Sample> {
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
