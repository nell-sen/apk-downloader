package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark Mode Glass Palette
val DarkBackground = Color(0xFF070B14)
val DarkSurface = Color(0xFF10192A)
val DarkGlassCard = Color(0x99121E33)
val DarkGlassCardBorder = Color(0x3360A5FA)
val DarkGlassHighlight = Color(0x4038BDF8)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)

// Light Mode Glass Palette
val LightBackground = Color(0xFFF1F5F9)
val LightSurface = Color(0xFFFFFFFF)
val LightGlassCard = Color(0xB3FFFFFF)
val LightGlassCardBorder = Color(0x4D94A3B8)
val LightGlassHighlight = Color(0x330284C7)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

// Brand & Accent Colors
val AccentCyan = Color(0xFF00E5FF)
val AccentBlue = Color(0xFF3B82F6)
val AccentIndigo = Color(0xFF6366F1)
val AccentPurple = Color(0xFF8B5CF6)
val AccentGreen = Color(0xFF10B981)
val AccentRed = Color(0xFFEF4444)
val AccentAmber = Color(0xFFF59E0B)

val PrimaryGlassGradient = Brush.linearGradient(
    colors = listOf(AccentCyan, AccentBlue, AccentIndigo)
)

val GlowGradient = Brush.radialGradient(
    colors = listOf(Color(0x3300E5FF), Color(0x00000000))
)

object GlassTokens {
    val CornerRadiusSm = 12.dp
    val CornerRadiusMd = 20.dp
    val CornerRadiusLg = 28.dp
    val BorderWidth = 1.dp
    val ShadowElevation = 8.dp
    val MinTouchTarget = 48.dp
}
