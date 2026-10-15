package com.voltap.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class VtColors(
    val bg: Color,
    val card: Color,
    val border: Color,
    val text: Color,
    val sub: Color,
    val accent: Color,
    val accentSoft: Color,
    val segBg: Color,
    val track: Color,
    val paused: Color,
    val warnBg: Color,
    val warnText: Color,
    val danger: Color,
)

val LightVt = VtColors(
    bg = Color(0xFFF4F5FA),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFECEEF7),
    text = Color(0xFF12131F),
    sub = Color(0xFF5F6378),
    accent = Color(0xFF4B53E0),
    accentSoft = Color(0xFFE9EAFD),
    segBg = Color(0xFFF0F1F8),
    track = Color(0xFFCDD0E0),
    paused = Color(0xFF5A5F8F),
    warnBg = Color(0xFFFFF4E0),
    warnText = Color(0xFF6B4200),
    danger = Color(0xFFC2415F),
)

val DarkVt = VtColors(
    bg = Color(0xFF0F1019),
    card = Color(0xFF1A1B2B),
    border = Color(0xFF26283D),
    text = Color(0xFFF2F3FA),
    sub = Color(0xFF9A9EB8),
    accent = Color(0xFF6B73F2),
    accentSoft = Color(0xFF262850),
    segBg = Color(0xFF24263A),
    track = Color(0xFF3A3D57),
    paused = Color(0xFF3A3D5C),
    warnBg = Color(0xFF3A2C12),
    warnText = Color(0xFFFFD99A),
    danger = Color(0xFFFF7C9A),
)

val LocalVt = staticCompositionLocalOf { LightVt }

@Composable
fun VolTapTheme(theme: Int, content: @Composable () -> Unit) {
    val dark = when (theme) {
        1 -> true
        2 -> isSystemInDarkTheme()
        else -> false
    }
    CompositionLocalProvider(LocalVt provides if (dark) DarkVt else LightVt) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}
