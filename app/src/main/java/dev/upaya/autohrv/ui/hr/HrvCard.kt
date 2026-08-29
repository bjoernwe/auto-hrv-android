package dev.upaya.autohrv.ui.hr

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.upaya.autohrv.ui.hr.charts.AcfChartHorizontalInset
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
internal fun HrvCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        content = content,
    )
}

/**
 * Stands in for [dev.upaya.autohrv.ui.hr.charts.AutoCorrelationChart] until [elapsedSeconds] of R–R
 * history reaches [windowSeconds] and a first estimate can be computed. The fill tracks that
 * progress along the same horizontal inset the real chart's zero line sits on, so the bar reads as
 * a preview of that line rather than a generic loader.
 */
@Composable
internal fun ChartPlaceholder(
    elapsedSeconds: Int,
    windowSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (windowSeconds > 0) (elapsedSeconds.toFloat() / windowSeconds).coerceIn(0f, 1f) else 0f
    val animatedProgress by
        animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 500),
            label = "acfPlaceholderProgress",
        )

    // The ACF curve itself is heart-derived (see AutoCorrelationChart), so the collecting
    // R–R history that will feed it uses the same warm tone ahead of time.
    val heart = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val pulse = rememberInfiniteTransition(label = "acfPlaceholderPulse")
    val pulseAlpha by
        pulse.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "acfPlaceholderPulseAlpha",
        )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp),
            ) {
                val padL = AcfChartHorizontalInset.toPx()
                val padR = AcfChartHorizontalInset.toPx()
                val trackWidth = size.width - padL - padR
                val y = size.height / 2f

                drawLine(
                    color = track,
                    start = Offset(padL, y),
                    end = Offset(size.width - padR, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                if (animatedProgress > 0f) {
                    val fillEnd = padL + trackWidth * animatedProgress
                    drawLine(
                        color = heart,
                        start = Offset(padL, y),
                        end = Offset(fillEnd, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    // Surface ring + pulsing dot at the fill's leading edge — a heartbeat blip
                    // marking the live edge of collection, echoing the peak marker in the real chart.
                    drawCircle(color = surface, radius = 5.dp.toPx(), center = Offset(fillEnd, y))
                    drawCircle(
                        color = heart.copy(alpha = pulseAlpha),
                        radius = 3.dp.toPx(),
                        center = Offset(fillEnd, y),
                    )
                }
            }

            val displaySeconds = elapsedSeconds.coerceIn(0, windowSeconds)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "$displaySeconds",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = heart,
                        ),
                )
                Text(
                    text = "of ${windowSeconds}s collected",
                    style = MaterialTheme.typography.labelSmall.copy(color = muted.copy(alpha = 0.7f)),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0EL, name = "ChartPlaceholder — collecting")
@Composable
private fun ChartPlaceholderPreview() {
    AutoHrvTheme {
        HrvCard {
            ACFHeader()
            Spacer(Modifier.height(6.dp))
            ChartPlaceholder(
                elapsedSeconds = 12,
                windowSeconds = 24,
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.14.em,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
    )
}
