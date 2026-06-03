package dev.upaya.autohrv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PolarRed = Color(0xFFD30024)

private val LightColors = lightColorScheme(
    primary = PolarRed,
    onPrimary = Color.White,
)

@Composable
fun AutoHrvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
