package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark Mode Native Palette
val DarkBackground = Color(0xFF0F1115) // Deep dark gray/blue
val DarkSurface = Color(0xFF1A1D24)
val DarkGlassCard = Color(0xFF22262F) // Solid card color
val DarkGlassCardBorder = Color(0x1AFFFFFF) // Very subtle border if needed
val DarkGlassHighlight = Color(0x00000000)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Light Mode Native Palette
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightGlassCard = Color(0xFFFFFFFF)
val LightGlassCardBorder = Color(0xFFE2E8F0)
val LightGlassHighlight = Color(0x00000000)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)

// Brand & Accent Colors
val AccentCyan = Color(0xFF007AFF) // Use a native blue instead of neon cyan
val AccentBlue = Color(0xFF007AFF)
val AccentIndigo = Color(0xFF5856D6)
val AccentPurple = Color(0xFFAF52DE)
val AccentGreen = Color(0xFF34C759)
val AccentRed = Color(0xFFFF3B30)
val AccentAmber = Color(0xFFFF9500)

val PrimaryGlassGradient = Brush.linearGradient(
    colors = listOf(AccentBlue, AccentIndigo)
)

val GlowGradient = Brush.radialGradient(
    colors = listOf(Color.Transparent, Color.Transparent)
)

object GlassTokens {
    val CornerRadiusSm = 8.dp
    val CornerRadiusMd = 16.dp
    val CornerRadiusLg = 24.dp
    val BorderWidth = 1.dp
    val ShadowElevation = 0.dp // Remove heavy shadows
    val MinTouchTarget = 48.dp
}
