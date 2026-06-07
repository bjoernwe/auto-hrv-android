package dev.upaya.autohrv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AutoHrvAccent = Color(0xFF1FD3E0)
val AutoHrvOnAccent = Color(0xFF06181B)
val AutoHrvBg = Color(0xFF0A0B0E)
val AutoHrvSurface = Color(0xFF131519)
val AutoHrvSurface2 = Color(0xFF1A1D23)
val AutoHrvOnSurface = Color(0xFFECEFF3)
val AutoHrvMuted = Color(0xFF9298A2)
val AutoHrvFaint = Color(0xFF5C626C)
val AutoHrvOutline = Color(0x12FFFFFF)
val AutoHrvOutlineStrong = Color(0x21FFFFFF)

private val DarkColors = darkColorScheme(
    primary = AutoHrvAccent,
    onPrimary = AutoHrvOnAccent,
    background = AutoHrvBg,
    surface = AutoHrvSurface,
    surfaceVariant = AutoHrvSurface2,
    onSurface = AutoHrvOnSurface,
    onSurfaceVariant = AutoHrvMuted,
    outline = AutoHrvOutlineStrong,
    outlineVariant = AutoHrvOutline,
)

@Composable
fun AutoHrvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
