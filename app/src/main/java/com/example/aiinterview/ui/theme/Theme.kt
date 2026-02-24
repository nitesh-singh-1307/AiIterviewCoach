package com.aiinterviewcoach.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// ──────────────────────────────────────────────────────────────────────────────
// Color tokens — deep navy terminal aesthetic
// ──────────────────────────────────────────────────────────────────────────────

val BgDeep        = Color(0xFF080C12)
val BgSurface     = Color(0xFF0D1117)
val BgCard        = Color(0xFF161B22)
val BgCardAlt     = Color(0xFF1C2128)
val BorderColor   = Color(0xFF30363D)
val AccentBlue    = Color(0xFF58A6FF)
val AccentGreen   = Color(0xFF3FB950)
val AccentAmber   = Color(0xFFD29922)
val AccentRed     = Color(0xFFF85149)
val AccentCyan    = Color(0xFF79C0FF)
val TextPrimary   = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)
val TextMuted     = Color(0xFF484F58)
val UserBubble    = Color(0xFF1A3A5C)

private val DarkColors = darkColorScheme(
    primary         = AccentBlue,
    onPrimary       = BgDeep,
    secondary       = AccentCyan,
    background      = BgDeep,
    surface         = BgSurface,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary,
    error           = AccentRed,
    outline         = BorderColor
)

private val AppTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Black,    fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 10.sp, lineHeight = 14.sp),
)

@Composable
fun AICoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = AppTypography,
        content     = content
    )
}
