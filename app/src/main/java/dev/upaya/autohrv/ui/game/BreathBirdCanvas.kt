package dev.upaya.autohrv.ui.game

import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// ── Design tokens ────────────────────────────────────────────────────────────

private val SkyTop    = Color(0xFF241A3A)
private val SkyMauve  = Color(0xFF5B3A52)
private val SkyClay   = Color(0xFFA85A52)
private val SkyPeach  = Color(0xFFF0A86A)
private val Gold      = Color(0xFFFFD98A)
private val Amber     = Color(0xFFFFCE8A)
private val Sand      = Color(0xFFF6B27A)
private val Coral     = Color(0xFFF58E73)
private val Cream     = Color(0xFFFFF4DF)
private val BirdBody  = Color(0xFFFFF3C8)
private val BirdOuter = Color(0xFFFFB454)
private val BirdBelly = Color(0xFFFFF6E0)
private val BirdBeak  = Color(0xFFFF9233)
private val BirdEye   = Color(0xFF2A1A10)
private val MissTint  = Color(0xFF9A6B78)

private val confettiColors = listOf(Gold, Coral, Cream, Sand)

// ── Particle ─────────────────────────────────────────────────────────────────

private data class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var life: Float, val max: Float,
    val size: Float, val colorIdx: Int,
)

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun BreathBirdCanvas(
    periodSec: Float,
    hrvQuality: Float,
    modifier: Modifier = Modifier,
    tailBreaths: Float = 2.5f,
    reduceMotion: Boolean = false,
) {
    val particles    = remember { mutableStateListOf<Particle>() }
    val ringHits     = remember { mutableStateMapOf<Int, Boolean>() }
    var prevBeat     by remember { mutableStateOf(Int.MIN_VALUE) }
    var frameNanos   by remember { mutableLongStateOf(System.nanoTime()) }
    val startNanos   = remember { System.nanoTime() }
    var canvasSize   by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            withFrameNanos { nanos ->
                val W = canvasSize.width.takeIf { it > 0f } ?: run { frameNanos = nanos; return@withFrameNanos }
                val H = canvasSize.height.takeIf { it > 0f } ?: run { frameNanos = nanos; return@withFrameNanos }
                val t  = (nanos - startNanos) / 1_000_000_000f
                val dt = 1f / 60f

                // update particles
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.life += dt; p.x += p.vx * dt; p.y += p.vy * dt
                    p.vy += 120f * dt; p.vx *= 0.96f
                    if (p.life >= p.max) iter.remove()
                }

                // scoring
                val g     = geometry(W, H, periodSec, tailBreaths)
                val q     = hrvQuality.coerceIn(0f, 1f)
                val phase = (g.omega * (t - g.lag) - PI.toFloat() / 2f) / PI.toFloat()
                val fl    = floor(phase).toInt()
                if (prevBeat != Int.MIN_VALUE && fl != prevBeat) {
                    val birdY = birdYAt(g, q, t)
                    val err   = abs(birdY - yTarget(g, t - g.lag))
                    val inZone = err < g.amp * 0.17f
                    ringHits[fl] = inZone
                    if (inZone) spawnBurst(particles, g.birdX, birdY)
                    ringHits.keys.filter { it < fl - 6 }.forEach { ringHits.remove(it) }
                }
                prevBeat = fl

                frameNanos = nanos
            }
        }
    }

    val t = if (reduceMotion) 3.4f else (frameNanos - startNanos) / 1_000_000_000f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        val W = size.width; val H = size.height
        drawSky(W, H, t)
        drawTrail(W, H, t, periodSec, hrvQuality, tailBreaths)
        drawStream(W, H, t, periodSec, tailBreaths)
        drawRings(W, H, t, periodSec, tailBreaths, ringHits)
        drawParticles(particles)
        drawBird(W, H, t, periodSec, hrvQuality, tailBreaths)
        drawPacer(W, H, t, periodSec)
    }
}

// ── Physics ───────────────────────────────────────────────────────────────────

private data class Geom(
    val midY: Float, val amp: Float, val omega: Float,
    val pacerX: Float, val birdX: Float, val v: Float, val lag: Float,
)

private fun geometry(W: Float, H: Float, periodSec: Float, tailBreaths: Float): Geom {
    val midY   = H * 0.5f
    val amp    = H * 0.27f
    val omega  = (2f * PI.toFloat()) / periodSec
    val pacerX = W * 0.87f
    val lag    = 2.0f
    val tb     = max(0.6f, tailBreaths)
    val birdX  = pacerX / (1f + lag / (tb * periodSec))
    val v      = (birdX / tb) / periodSec
    return Geom(midY, amp, omega, pacerX, birdX, v, lag)
}

private fun yTarget(g: Geom, tau: Float) = g.midY - g.amp * sin(g.omega * tau)

private fun birdYAt(g: Geom, q: Float, tau: Float): Float {
    val wobble = (1f - q) * g.amp * (
        0.42f * sin(1.6f * tau) +
        0.26f * sin(2.7f * tau + 1.1f) +
        0.16f * sin(4.3f * tau + 2.0f) +
        0.12f * sin(7.2f * tau + 0.6f)
    )
    return g.midY - g.amp * (0.5f + 0.5f * q) * sin(g.omega * (tau - g.lag)) + wobble
}

private fun birdTilt(g: Geom, q: Float, t: Float): Float {
    val dt = 0.02f
    val vy = (birdYAt(g, q, t + dt) - birdYAt(g, q, t - dt)) / (2f * dt)
    return atan2(vy, g.v * 1.8f).coerceIn(-0.5f, 0.5f) * 0.85f
}

// ── Particles ─────────────────────────────────────────────────────────────────

private fun spawnBurst(particles: MutableList<Particle>, x: Float, y: Float) {
    repeat(14) { i ->
        val a  = (2f * PI.toFloat() * i / 14f) + Random.nextFloat() * 0.5f
        val sp = 30f + Random.nextFloat() * 80f
        particles.add(
            Particle(
                x = x, y = y,
                vx = cos(a) * sp, vy = sin(a) * sp - 18f,
                life = 0f, max = 0.6f + Random.nextFloat() * 0.5f,
                size = 1.5f + Random.nextFloat() * 2.5f,
                colorIdx = i % confettiColors.size,
            )
        )
    }
}

// ── Draw: sky ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawSky(W: Float, H: Float, t: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(0f to SkyTop, 0.42f to SkyMauve, 0.72f to SkyClay, 1f to SkyPeach),
            startY = 0f, endY = H,
        ),
        size = Size(W, H),
    )
    // sun glow
    val sx = W * 0.74f; val sy = H * 0.84f; val sr = H * 0.75f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to Amber.copy(alpha = 0.5f), 0.4f to Amber.copy(alpha = 0.16f), 1f to Color.Transparent),
            center = Offset(sx, sy), radius = sr,
        ),
        radius = sr, center = Offset(sx, sy),
    )
    // drifting clouds
    val drift = (t * 9f) % (W + 200f)
    fun cloudX(base: Float, speed: Float) = ((base - drift * speed).mod(W + 200f) + W + 200f).mod(W + 200f) - 100f
    drawCloud(cloudX(W * 0.2f, 1f), H * 0.2f, 0.9f, 0.12f)
    drawCloud(cloudX(W * 0.85f, 0.6f), H * 0.32f, 0.7f, 0.09f)
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, sc: Float, alpha: Float) {
    listOf(Triple(0f, 0f, 22f), Triple(18f, 4f, 16f), Triple(-18f, 4f, 15f), Triple(6f, -6f, 14f))
        .forEach { (dx, dy, r) ->
            drawOval(
                color = Color(0xFFFFF1E2).copy(alpha = alpha),
                topLeft = Offset(cx + dx * sc - r * sc, cy + dy * sc - r * sc * 0.7f),
                size = Size(r * sc * 2f, r * sc * 1.4f),
            )
        }
}

// ── Draw: trail ───────────────────────────────────────────────────────────────

private fun DrawScope.drawTrail(
    W: Float, H: Float, t: Float,
    periodSec: Float, hrvQuality: Float, tailBreaths: Float,
) {
    val g      = geometry(W, H, periodSec, tailBreaths)
    val q      = hrvQuality.coerceIn(0f, 1f)
    val inZone = abs(birdYAt(g, q, t) - yTarget(g, t - g.lag)) < g.amp * 0.17f

    val pts = ArrayList<Offset>(((g.birdX / 4f).toInt() + 1))
    var x = 0f
    while (x <= g.birdX) {
        pts.add(Offset(x, birdYAt(g, q, t - (g.birdX - x) / g.v)))
        x += 4f
    }
    if (pts.size < 2) return

    drawIntoCanvas { canvas ->
        val ap = android.graphics.Path()
        ap.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) ap.lineTo(pts[i].x, pts[i].y)

        val paint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f * density
            strokeJoin  = android.graphics.Paint.Join.ROUND
            strokeCap   = android.graphics.Paint.Cap.ROUND
            isAntiAlias = true
            shader = android.graphics.LinearGradient(
                0f, 0f, g.birdX, 0f,
                intArrayOf(
                    Color(0xFFFFD9A0).copy(alpha = 0f).toArgb(),
                    Color(0xFFFFE2B0).copy(alpha = if (inZone) 0.55f else 0.4f).toArgb(),
                    Cream.copy(alpha = if (inZone) 0.95f else 0.8f).toArgb(),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        }
        canvas.nativeCanvas.drawPath(ap, paint)

        // glow pass (wider, lower alpha)
        val glowPaint = android.graphics.Paint(paint).apply {
            strokeWidth = (if (inZone) 14f else 9f) * density
            alpha = (if (inZone) 60 else 35)
            shader = null
            color = Amber.toArgb()
            maskFilter = android.graphics.BlurMaskFilter(
                (if (inZone) 14f else 9f) * density,
                android.graphics.BlurMaskFilter.Blur.NORMAL,
            )
        }
        canvas.nativeCanvas.drawPath(ap, glowPaint)
    }

    // beat dots
    for (i in pts.size - 1 downTo 0 step 9) {
        val a = (pts[i].x / g.birdX) * 0.5f
        drawCircle(color = Cream.copy(alpha = a), radius = 1.4f * density, center = pts[i])
    }
}

// ── Draw: sparkle stream ──────────────────────────────────────────────────────

private fun DrawScope.drawStream(
    W: Float, H: Float, t: Float,
    periodSec: Float, tailBreaths: Float,
) {
    val g    = geometry(W, H, periodSec, tailBreaths)
    val span = g.pacerX - g.birdX
    var x    = g.birdX + 8f
    while (x <= g.pacerX) {
        val y  = yTarget(g, t - (g.pacerX - x) / g.v)
        val f  = (x - g.birdX) / span
        val tw = 0.55f + 0.45f * sin(t * 4f + x * 0.25f)
        val a  = (0.10f + 0.34f * f) * tw
        val r  = (1.1f + 1.5f * f) * density
        // glow halo
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Amber.copy(alpha = a * 0.8f), 1f to Color.Transparent),
                center = Offset(x, y), radius = r * 3f,
            ),
            radius = r * 3f, center = Offset(x, y),
        )
        drawCircle(color = Color(0xFFFFE2A8).copy(alpha = a), radius = r, center = Offset(x, y))
        x += 15f
    }
}

// ── Draw: rings ───────────────────────────────────────────────────────────────

private fun DrawScope.drawRings(
    W: Float, H: Float, t: Float,
    periodSec: Float, tailBreaths: Float,
    ringHits: Map<Int, Boolean>,
) {
    val g    = geometry(W, H, periodSec, tailBreaths)
    val rx   = 15f * density
    val ry   = 29f * density
    val nMax = floor((g.omega * t - PI.toFloat() / 2f) / PI.toFloat()).toInt()

    for (n in nMax downTo nMax - 60) {
        val tau = (PI.toFloat() / 2f + n * PI.toFloat()) / g.omega
        val x   = g.pacerX - g.v * (t - tau)
        if (x < -rx - 6f) break
        if (x > g.pacerX + 2f) continue

        val y      = g.midY - g.amp * if (n % 2 == 0) 1f else -1f
        val passed = x < g.birdX - 2f

        if (passed) {
            val hit  = ringHits[n]
            val tint = if (hit == false) MissTint.copy(alpha = 0.7f * 0.26f) else Sand.copy(alpha = 0.7f * 0.26f)
            drawIntoCanvas { canvas ->
                val p = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 3f * density; isAntiAlias = true; color = tint.toArgb()
                }
                canvas.nativeCanvas.drawOval(x - rx, y - ry, x + rx, y + ry, p)
            }
            continue
        }

        // catch-zone fill
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color(0xFFFFE2A8).copy(alpha = 0.14f), 1f to Color.Transparent),
                center = Offset(x, y), radius = ry,
            ),
            radius = ry, center = Offset(x, y),
        )
        // outer glow ring
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color(0xFFFFCF8A).copy(alpha = 0.35f), 1f to Color.Transparent),
                center = Offset(x, y), radius = ry + 10f * density,
            ),
            radius = ry + 10f * density, center = Offset(x, y),
        )
        // outer hoop
        drawIntoCanvas { canvas ->
            val p = android.graphics.Paint().apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 4.5f * density; isAntiAlias = true
                shader = android.graphics.LinearGradient(
                    x - rx, 0f, x + rx, 0f,
                    intArrayOf(Coral.toArgb(), Gold.toArgb(), Sand.toArgb()),
                    floatArrayOf(0f, 0.5f, 1f),
                    android.graphics.Shader.TileMode.CLAMP,
                )
            }
            canvas.nativeCanvas.drawOval(x - rx, y - ry, x + rx, y + ry, p)
        }
        // inner highlight
        drawIntoCanvas { canvas ->
            val p = android.graphics.Paint().apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1.4f * density; isAntiAlias = true
                color = Cream.copy(alpha = 0.7f).toArgb()
            }
            canvas.nativeCanvas.drawOval(x - rx, y - ry, x + rx, y + ry, p)
        }
    }
}

// ── Draw: confetti ────────────────────────────────────────────────────────────

private fun DrawScope.drawParticles(particles: List<Particle>) {
    particles.forEach { p ->
        val a   = 1f - p.life / p.max
        val col = confettiColors[p.colorIdx].copy(alpha = a)
        val s   = p.size * density
        withTransform({
            translate(p.x, p.y)
            rotate(p.life * 6f * (180f / PI.toFloat()))
        }) {
            drawRect(col, topLeft = Offset(-s, -s), size = Size(s * 2f, s * 2f))
        }
    }
}

// ── Draw: bird ────────────────────────────────────────────────────────────────

private fun DrawScope.drawBird(
    W: Float, H: Float, t: Float,
    periodSec: Float, hrvQuality: Float, tailBreaths: Float,
) {
    val g      = geometry(W, H, periodSec, tailBreaths)
    val q      = hrvQuality.coerceIn(0f, 1f)
    val birdY  = birdYAt(g, q, t)
    val tilt   = birdTilt(g, q, t)
    val inZone = abs(birdY - yTarget(g, t - g.lag)) < g.amp * 0.17f
    val d      = density

    withTransform({ translate(g.birdX, birdY) }) {
        if (inZone) {
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(0f to Color(0xFFFFE6A8).copy(alpha = 0.5f), 1f to Color.Transparent),
                    center = Offset.Zero, radius = 26f * d,
                ),
                radius = 26f * d, center = Offset.Zero,
            )
        }
        withTransform({ rotate(tilt * 0.7f * (180f / PI.toFloat())) }) {
            // body
            drawOval(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(0f to BirdBody, 1f to BirdOuter),
                    center = Offset(-3f * d, -3f * d), radius = 14f * d,
                ),
                topLeft = Offset(-12f * d, -10.5f * d),
                size = Size(24f * d, 21f * d),
            )
            // belly
            drawOval(
                color = BirdBelly,
                topLeft = Offset(-6.5f * d, -3.5f * d),
                size = Size(15f * d, 13f * d),
            )
            // wing
            withTransform({
                translate(-2f * d, -1f * d)
                rotate(sin(t * 6f) * 0.5f * (180f / PI.toFloat()))
            }) {
                drawOval(
                    color = Coral,
                    topLeft = Offset(-7f * d, -4.5f * d),
                    size = Size(14f * d, 9f * d),
                )
            }
            // beak
            drawPath(
                Path().apply {
                    moveTo(11f * d, -1f * d); lineTo(18f * d, 1.5f * d); lineTo(11f * d, 3.5f * d); close()
                },
                BirdBeak,
            )
            // eye
            drawCircle(BirdEye, radius = 2.4f * d, center = Offset(6f * d, -3f * d))
            drawCircle(Color.White, radius = 0.8f * d, center = Offset(6.8f * d, -3.7f * d))
        }
    }
}

// ── Draw: pacer ───────────────────────────────────────────────────────────────

private fun DrawScope.drawPacer(W: Float, H: Float, t: Float, periodSec: Float) {
    val omega  = (2f * PI.toFloat()) / periodSec
    val cx     = W * 0.87f
    val cy     = H * 0.5f - H * 0.27f * sin(omega * t)
    val d      = density

    // halo
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f   to Color(0xFFFFE9B8).copy(alpha = 0.5f),
                0.5f to Amber.copy(alpha = 0.16f),
                1f   to Color.Transparent,
            ),
            center = Offset(cx, cy), radius = 30f * d,
        ),
        radius = 30f * d, center = Offset(cx, cy),
    )
    // twinkling particles
    for (i in 0 until 16) {
        val ang = i * 12.9898f + t * (0.6f + (i % 3) * 0.25f)
        val rad = (5f + (i * 7 % 12).toFloat() + sin(t * 2f + i) * 2.5f) * d
        val px  = cx + cos(ang) * rad
        val py  = cy + sin(ang) * rad * 0.9f
        val tw  = 0.4f + 0.6f * abs(sin(t * 3.5f + i * 1.7f))
        val r   = (0.9f + 1.6f * tw) * d
        // glow
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Gold.copy(alpha = 0.5f * tw), 1f to Color.Transparent),
                center = Offset(px, py), radius = r * 4f,
            ),
            radius = r * 4f, center = Offset(px, py),
        )
        drawCircle(color = Cream.copy(alpha = 0.85f * tw), radius = r, center = Offset(px, py))
    }
    // bright core — glow + dot
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to Color(0xFFFFE6B0).copy(alpha = 0.8f), 1f to Color.Transparent),
            center = Offset(cx, cy), radius = 12f * d,
        ),
        radius = 12f * d, center = Offset(cx, cy),
    )
    drawCircle(color = Color(0xFFFFF8EA), radius = 3.2f * d, center = Offset(cx, cy))
}
