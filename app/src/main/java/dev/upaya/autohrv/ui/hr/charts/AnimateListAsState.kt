package dev.upaya.autohrv.ui.hr.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.lerp

@Composable
fun animateListAsState(
    target: List<Float>,
    animationSpec: AnimationSpec<Float> = tween(5000),
): List<Float> {
    var prev by remember { mutableStateOf(target) }
    var current by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(target) {
        prev = prev.zip(current) { a, b -> lerp(a, b, progress.value) }
        current = target
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec)
    }

    return prev.zip(current) { a, b -> lerp(a, b, progress.value) }
}
