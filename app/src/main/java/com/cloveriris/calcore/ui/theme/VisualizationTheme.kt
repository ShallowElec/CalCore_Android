package com.cloveriris.calcore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 可视化舞台专用颜色体系（独立于 MaterialTheme，但映射自当前主题）
 *
 * L1-L8 全部可视化组件通过 [LocalVisualizationColors] 取色，不再硬编码 TerminalGreen。
 * 三套映射：
 * - 系统动态色：从 MaterialTheme.colorScheme 提取 primary/secondary
 * - 终端绿 (Calcore Dark)：#0a0a0a 底 + #00ff41 强调
 * - 浅蓝白 (Calcore Light)：#FFFFFF 底 + #2196F3 / #4FC3F7 强调
 */
data class VisualizationColorScheme(
    /** 可视化舞台整体背景 */
    val stageBg: Color,
    /** 子面板背景（略浅/略深于 stageBackground） */
    val surface: Color,
    /** 主数据色：已分配方块、信号流、粒子、流光 */
    val dataPrimary: Color,
    /** 次数据色：数据路径渐变、次要高亮 */
    val dataSecondary: Color,
    /** 强调色：警告、进位、光标、栈顶、活跃节点 */
    val accent: Color,
    /** 网格线、虚线、细边框 */
    val gridLine: Color,
    /** 暗淡文字、标签、注释 */
    val textMuted: Color,
    /** 数据方块（实心）上的文字颜色 */
    val textOnData: Color,
    /** 背景上的主要文字颜色 */
    val textOnBackground: Color,
    /** 光标/读取头/指针高亮 */
    val cursor: Color,
    /** 发光/拖尾/脉冲外圈 */
    val glow: Color,
    /** 空单元格/未分配内存/待机方块背景 */
    val cellEmpty: Color,
    /** 单元格/方块边框 */
    val cellBorder: Color,
    /** 是否启用 CRT 扫描线效果 */
    val crtEnabled: Boolean
)

/**
 * 终端绿配色方案（Calcore Dark，默认硬核风格）
 */
val TerminalVisualizationScheme = VisualizationColorScheme(
    stageBg = TerminalBackground,
    surface = TerminalSurface,
    dataPrimary = TerminalGreen,
    dataSecondary = TerminalGreenDark,
    accent = TerminalAmber,
    gridLine = TerminalGridLine,
    textMuted = TerminalGray,
    textOnData = Color.Black,
    textOnBackground = TerminalOnBackground,
    cursor = TerminalAmber,
    glow = TerminalGreen,
    cellEmpty = Color(0xFF1A1A1A),
    cellBorder = Color(0xFF2A2A2A),
    crtEnabled = true
)

/**
 * 浅蓝白配色方案（Calcore Light）
 */
val LightVisualizationScheme = VisualizationColorScheme(
    stageBg = LightBackground,
    surface = LightSurface,
    dataPrimary = DeepBlue,
    dataSecondary = SkyBlue,
    accent = Color(0xFFFFA657), // 保持琥珀色作为警告/进位色，在浅色下依然醒目
    gridLine = Color(0xFFE3F2FD),
    textMuted = Color(0xFF8B949E),
    textOnData = Color.White,
    textOnBackground = Color(0xFF1A1A1A),
    cursor = DeepBlue,
    glow = SkyBlue,
    cellEmpty = Color(0xFFF5F5F5),
    cellBorder = Color(0xFFE0E0E0),
    crtEnabled = false
)

/**
 * 从当前 MaterialTheme 动态提取可视化配色（用于 SYSTEM_DYNAMIC 主题）
 */
@Composable
fun dynamicVisualizationScheme(darkTheme: Boolean): VisualizationColorScheme {
    val scheme = MaterialTheme.colorScheme
    return if (darkTheme) {
        VisualizationColorScheme(
            stageBg = Color(0xFF0A0A0A),
            surface = Color(0xFF121212),
            dataPrimary = scheme.primary,
            dataSecondary = scheme.primary.copy(alpha = 0.7f),
            accent = scheme.secondary,
            gridLine = Color(0xFF1F1F1F),
            textMuted = Color(0xFF8B949E),
            textOnData = Color.Black,
            textOnBackground = Color(0xFFE0E0E0),
            cursor = scheme.secondary,
            glow = scheme.primary,
            cellEmpty = Color(0xFF1A1A1A),
            cellBorder = Color(0xFF2A2A2A),
            crtEnabled = true
        )
    } else {
        VisualizationColorScheme(
            stageBg = scheme.background,
            surface = scheme.surface,
            dataPrimary = scheme.primary,
            dataSecondary = scheme.primary.copy(alpha = 0.7f),
            accent = scheme.secondary,
            gridLine = Color(0xFFE0E0E0),
            textMuted = Color(0xFF8B949E),
            textOnData = Color.White,
            textOnBackground = scheme.onBackground,
            cursor = scheme.primary,
            glow = scheme.primary.copy(alpha = 0.5f),
            cellEmpty = Color(0xFFF5F5F5),
            cellBorder = Color(0xFFE0E0E0),
            crtEnabled = false
        )
    }
}

/**
 * CompositionLocal，供所有可视化组件读取当前配色
 */
val LocalVisualizationColors = staticCompositionLocalOf {
    TerminalVisualizationScheme // 默认回退
}

/**
 * 在 CalcoreTheme 内部包裹可视化颜色注入
 */
@Composable
fun ProvideVisualizationColors(
    scheme: VisualizationColorScheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalVisualizationColors provides scheme,
        content = content
    )
}


