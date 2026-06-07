package dev.upaya.autohrv.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@Composable
fun BreathBirdScreen(viewModel: BreathBirdViewModel) {
    val periodSec   by viewModel.periodSec.collectAsStateWithLifecycle()
    val hrvQuality  by viewModel.hrvQuality.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BreathBirdContent(periodSec = periodSec, hrvQuality = hrvQuality)
}

@Composable
private fun BreathBirdContent(periodSec: Float, hrvQuality: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        BreathBirdCanvas(
            periodSec  = periodSec,
            hrvQuality = hrvQuality,
            modifier   = Modifier.fillMaxSize(),
        )
        FollowHintPill(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 38.dp),
        )
    }
}

@Composable
private fun FollowHintPill(modifier: Modifier = Modifier) {
    val pillBg      = Color(0x80241E1A)
    val pillBorder  = Color(0x33FFD296)
    val dotColor    = Color(0xFFFFE6B0)
    val labelColor  = Color(0xD1FFF3E2)
    val shape       = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(pillBg)
            .border(1.dp, pillBorder, shape)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 9.dp)
                .size(8.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text       = "Follow the glimmer with your breath",
            fontSize   = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color      = labelColor,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF241A3AL)
@Composable
private fun BreathBirdContentPreview() {
    AutoHrvTheme {
        BreathBirdContent(periodSec = 10f, hrvQuality = 0.85f)
    }
}
