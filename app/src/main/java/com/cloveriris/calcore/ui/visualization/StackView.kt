package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalAmber
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.theme.TerminalGray

/**
 * 栈可视化组件
 *
 * @param frames 栈帧列表，从底到顶
 * @param spValue 栈指针值（高亮位置）
 */
data class StackFrameVisual(
    val label: String,
    val value: String,
    val isActive: Boolean = false
)

@Composable
fun StackView(
    frames: List<StackFrameVisual>,
    modifier: Modifier = Modifier,
    spValue: String = "RSP"
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((frames.size * 36 + 40).dp)
            .padding(8.dp)
    ) {
        val rowHeight = 32.dp.toPx()
        val gap = 4.dp.toPx()

        // 栈指针标签
        val spLayout = textMeasurer.measure(
            "▼ $spValue",
            TextStyle(
                color = TerminalAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        )

        frames.reversed().forEachIndexed { index, frame ->
            val y = index * (rowHeight + gap)
            val isTop = index == 0

            // 栈帧背景
            val bgColor = when {
                isTop -> TerminalGreen.copy(alpha = 0.2f)
                frame.isActive -> TerminalAmber.copy(alpha = 0.15f)
                else -> Color(0xFF1F1F1F)
            }

            drawRect(
                color = bgColor,
                topLeft = Offset(0f, y),
                size = Size(size.width * 0.7f, rowHeight)
            )

            // 边框
            if (isTop) {
                drawRect(
                    color = TerminalGreen,
                    topLeft = Offset(0f, y),
                    size = Size(size.width * 0.7f, rowHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            // 标签文字
            val labelLayout = textMeasurer.measure(
                frame.label,
                TextStyle(
                    color = if (isTop) TerminalGreen else TerminalGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                labelLayout,
                topLeft = Offset(8.dp.toPx(), y + (rowHeight - labelLayout.size.height) / 2)
            )

            // 值文字
            val valueLayout = textMeasurer.measure(
                frame.value,
                TextStyle(
                    color = if (isTop) TerminalGreen else TerminalGray.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                valueLayout,
                topLeft = Offset(
                    size.width * 0.4f,
                    y + (rowHeight - valueLayout.size.height) / 2
                )
            )
        }

        // 绘制栈指针箭头
        if (frames.isNotEmpty()) {
            drawText(
                spLayout,
                topLeft = Offset(size.width * 0.75f, 0f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun StackViewPreview() {
    CalcoreTheme {
        val frames = listOf(
            StackFrameVisual("main()", "0x7FFE_EFBC9000"),
            StackFrameVisual("fact(3)", "0x7FFE_EFBC8FE0", isActive = true),
            StackFrameVisual("fact(2)", "0x7FFE_EFBC8FC0", isActive = true),
            StackFrameVisual("fact(1)", "0x7FFE_EFBC8FA0", isActive = true)
        )
        StackView(frames = frames)
    }
}
