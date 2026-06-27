package dev.upaya.autohrv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Two-tone identity: a cool tone for the breath side (orb, pace, resonance)
// and a warm tone for the heart side (R–R trace, RMSSD, heart rate).
val AutoHrvBreath = Color(0xFF1FD3E0)
val AutoHrvOnBreath = Color(0xFF06181B)
val AutoHrvHeart = Color(0xFFFF6B7D)
val AutoHrvOnHeart = Color(0xFF2A080C)
val AutoHrvBg = Color(0xFF0A0B0E)
val AutoHrvSurface = Color(0xFF131519)
val AutoHrvSurface2 = Color(0xFF1A1D23)
val AutoHrvOnSurface = Color(0xFFECEFF3)
val AutoHrvMuted = Color(0xFF9298A2)
val AutoHrvFaint = Color(0xFF5C626C)
val AutoHrvOutline = Color(0x12FFFFFF)
val AutoHrvOutlineStrong = Color(0x21FFFFFF)

private val DarkColors = darkColorScheme(
    primary = AutoHrvBreath,
    onPrimary = AutoHrvOnBreath,
    secondary = AutoHrvHeart,
    onSecondary = AutoHrvOnHeart,
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
