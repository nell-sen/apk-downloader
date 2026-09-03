package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GlassColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val glassCard: Color,
    val glassCardBorder: Color,
    val glassHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentCyan: Color = AccentCyan,
    val accentBlue: Color = AccentBlue,
    val accentGreen: Color = AccentGreen,
    val accentRed: Color = AccentRed,
    val accentAmber: Color = AccentAmber
)

val DarkGlassColors = GlassColorScheme(
    isDark = true,
    background = DarkBackground,
    surface = DarkSurface,
    glassCard = DarkGlassCard,
    glassCardBorder = DarkGlassCardBorder,
    glassHighlight = DarkGlassHighlight,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary
)

val LightGlassColors = GlassColorScheme(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    glassCard = LightGlassCard,
    glassCardBorder = LightGlassCardBorder,
    glassHighlight = LightGlassHighlight,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary
)

val LocalGlassColors = staticCompositionLocalOf { DarkGlassColors }

private val MaterialDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    primaryContainer = AccentBlue,
    onPrimaryContainer = Color.White,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary
)

private val MaterialLightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentCyan,
    onPrimaryContainer = Color.Black,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary
)

@Composable
fun NellDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val glassColors = if (darkTheme) DarkGlassColors else LightGlassColors
    val materialColors = if (darkTheme) MaterialDarkColorScheme else MaterialLightColorScheme

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
