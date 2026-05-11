package com.cloveriris.calcore.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Calcore 品牌色与可视化语义色（独立于 Material Theme）
// ============================================================

// --- 终端绿（可视化舞台通用语义色） ---
val TerminalGreen = Color(0xFF00FF41)
val TerminalGreenDark = Color(0xFF00CC33)
val TerminalGreenMuted = Color(0xFF006622)
val TerminalAmber = Color(0xFFFFA657)
val TerminalBackground = Color(0xFF0A0A0A)
val TerminalSurface = Color(0xFF121212)
val TerminalGridLine = Color(0xFF1F1F1F)
val TerminalOnBackground = Color(0xFFE0E0E0)
val TerminalGray = Color(0xFF8B949E)

// --- 浅蓝风（用户个人风格） ---
val SkyBlue = Color(0xFF4FC3F7)
val DeepBlue = Color(0xFF2196F3)
val IceBlue = Color(0xFFE1F5FE)
val SoftBlue = Color(0xFF81D4FA)
val OceanBlue = Color(0xFF0277BD)
val LightSurface = Color(0xFFF5F5F5)
val LightBackground = Color(0xFFFFFFFF)

// ============================================================
// Material 3 ColorScheme 定义
// ============================================================

internal val CalcoreDarkScheme = androidx.compose.material3.darkColorScheme(
    primary = TerminalGreen,
    onPrimary = Color.Black,
    primaryContainer = TerminalGreenDark,
    onPrimaryContainer = Color.White,
    secondary = TerminalAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D2E1F),
    onSecondaryContainer = TerminalAmber,
    tertiary = TerminalGray,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2D2D2D),
    onTertiaryContainer = TerminalGray,
    background = TerminalBackground,
    onBackground = TerminalOnBackground,
    surface = TerminalSurface,
    onSurface = TerminalOnBackground,
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = TerminalGray,
    surfaceTint = TerminalGreen,
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF3D1F1F),
    onErrorContainer = Color(0xFFFF8A80),
    outline = Color(0xFF3D3D3D),
    outlineVariant = Color(0xFF2A2A2A),
    scrim = Color.Black
)

internal val CalcoreLightScheme = androidx.compose.material3.lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = IceBlue,
    onPrimaryContainer = OceanBlue,
    secondary = SkyBlue,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = OceanBlue,
    tertiary = SoftBlue,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFF0F9FF),
    onTertiaryContainer = DeepBlue,
    background = LightBackground,
    onBackground = Color(0xFF1A1A1A),
    surface = LightSurface,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE3F2FD),
    onSurfaceVariant = Color(0xFF546E7A),
    surfaceTint = DeepBlue,
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = LightBackground,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    outline = Color(0xFFB0BEC5),
    outlineVariant = Color(0xFFE0E0E0),
    scrim = Color.Black
)
