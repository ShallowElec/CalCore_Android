package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.LocalVisualizationColors

/**
 * CRT 扫描线叠加效果
 *
 * 为可视化面板提供老式终端/示波器的质感：
 * - 水平扫描线（极细，低透明度）
 * - 垂直刷新光束（从上到下缓慢移动）
 * - 微妙的绿色色调渐变
 * - 边角暗角（vignette）
 */
@Composable
fun CrtScanlineOverlay(modifier: Modifier = Modifier) {
    val viz = LocalVisualizationColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "crt-scan")

    // 垂直刷新光束位置（0f ~ 1f，循环）
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan-y"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val lineCount = (size.height / 4.dp.toPx()).toInt().coerceIn(20, 120)
        val lineSpacing = size.height / lineCount

        // 1. 水平扫描线
        for (i in 0..lineCount) {
            val y = i * lineSpacing
            // 扫描线中心略亮，边缘暗
            val lineAlpha = 0.04f + 0.03f * kotlin.math.sin(i * 0.5f)
            drawLine(
                color = viz.dataPrimary.copy(alpha = lineAlpha * 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5f.dp.toPx()
            )
        }

        // 2. 垂直刷新光束（一道更亮的水平线从上到下扫过）
        val beamY = scanY * size.height
        val beamHeight = 3.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    viz.dataPrimary.copy(alpha = 0.06f),
                    viz.dataPrimary.copy(alpha = 0.12f),
                    viz.dataPrimary.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                startY = beamY - beamHeight * 2,
                endY = beamY + beamHeight * 2
            ),
            topLeft = Offset(0f, beamY - beamHeight * 2),
            size = androidx.compose.ui.geometry.Size(size.width, beamHeight * 4)
        )

        // 3. 暗角（四角渐暗，聚焦中心）
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.25f)
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width * 0.75f
            ),
            size = androidx.compose.ui.geometry.Size(size.width, size.height)
        )

        // 4. 顶部/底部边缘微光（模拟 CRT 荧光溢出）
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    viz.dataPrimary.copy(alpha = 0.03f),
                    Color.Transparent,
                    Color.Transparent,
                    viz.dataPrimary.copy(alpha = 0.03f)
                )
            ),
            size = androidx.compose.ui.geometry.Size(size.width, size.height)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun CrtScanlineOverlayPreview() {
    CalcoreTheme {
        CrtScanlineOverlay()
    }
}
