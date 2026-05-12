package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.presentation.visualization.AluOperationVisual
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.theme.TerminalAmber

/**
 * ALU 运算可视化（L3: ALU）
 *
 * @param operation 当前 ALU 运算，null 表示空闲
 */
@Composable
fun AluVisualizer(
    operation: AluOperationVisual?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "alu-flicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Column(
        modifier = modifier
            .background(TerminalBackground)
            .padding(8.dp)
    ) {
        Text(
            text = "L3: ALU",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            if (operation == null) {
                val idleLayout = textMeasurer.measure(
                    "ALU IDLE",
                    TextStyle(
                        color = TerminalGray,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                drawText(
                    idleLayout,
                    topLeft = Offset(
                        (size.width - idleLayout.size.width) / 2,
                        (size.height - idleLayout.size.height) / 2
                    )
                )
                return@Canvas
            }

            val boxWidth = 88.dp.toPx()
            val boxHeight = 44.dp.toPx()
            val opGap = 16.dp.toPx()
            val totalWidth = boxWidth * 3 + opGap * 4
            val startX = (size.width - totalWidth) / 2
            val centerY = size.height / 2

            val opSymbol = when (operation.operation) {
                "ADD" -> "+"
                "SUB" -> "-"
                "MUL" -> "×"
                "DIV" -> "÷"
                "AND" -> "&"
                "OR" -> "|"
                "XOR" -> "^"
                "NOT" -> "~"
                else -> operation.operation
            }

            val leftHex = operation.left.toString(16).uppercase().padStart(16, '0')
            val rightHex = operation.right.toString(16).uppercase().padStart(16, '0')
            val resultHex = operation.result.toString(16).uppercase().padStart(16, '0')

            // 整个 ALU 区域琥珀色闪烁背景
            if (operation.isActive) {
                drawRect(
                    color = TerminalAmber.copy(alpha = flickerAlpha * 0.12f),
                    topLeft = Offset(startX - opGap / 2, centerY - boxHeight / 2 - 6.dp.toPx()),
                    size = Size(totalWidth + opGap, boxHeight + 12.dp.toPx())
                )
                drawRect(
                    color = TerminalAmber.copy(alpha = flickerAlpha * 0.5f),
                    topLeft = Offset(startX - opGap / 2, centerY - boxHeight / 2 - 6.dp.toPx()),
                    size = Size(totalWidth + opGap, boxHeight + 12.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }

            // 左操作数
            drawHexBox(startX, centerY - boxHeight / 2, boxWidth, boxHeight, leftHex, textMeasurer)
            // 运算符
            drawAluOperator(startX + boxWidth + opGap / 2, centerY, opSymbol, textMeasurer)
            // 右操作数
            drawHexBox(startX + boxWidth + opGap, centerY - boxHeight / 2, boxWidth, boxHeight, rightHex, textMeasurer)
            // 等号
            drawAluOperator(startX + boxWidth * 2 + opGap * 1.5f, centerY, "=", textMeasurer)
            // 结果
            drawHexBox(startX + boxWidth * 2 + opGap * 2, centerY - boxHeight / 2, boxWidth, boxHeight, resultHex, textMeasurer, isResult = true)
        }
    }
}

private fun DrawScope.drawHexBox(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    hex: String,
    textMeasurer: TextMeasurer,
    isResult: Boolean = false
) {
    drawRect(
        color = if (isResult) TerminalGreen.copy(alpha = 0.08f) else Color(0xFF1F1F1F),
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    drawRect(
        color = if (isResult) TerminalGreen.copy(alpha = 0.4f) else TerminalAmber.copy(alpha = 0.3f),
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
    val textLayout = textMeasurer.measure(
        hex,
        TextStyle(
            color = TerminalGreen,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(
        textLayout,
        topLeft = Offset(
            x + (width - textLayout.size.width) / 2,
            y + (height - textLayout.size.height) / 2
        )
    )
}

private fun DrawScope.drawAluOperator(
    x: Float,
    y: Float,
    symbol: String,
    textMeasurer: TextMeasurer
) {
    val textLayout = textMeasurer.measure(
        symbol,
        TextStyle(
            color = TerminalGray,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(
        textLayout,
        topLeft = Offset(
            x - textLayout.size.width / 2,
            y - textLayout.size.height / 2
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun AluVisualizerPreview() {
    CalcoreTheme {
        AluOperationVisual(
            operation = "ADD",
            left = 0x0000_0000_0000_000AL,
            right = 0x0000_0000_0000_000BL,
            result = 0x0000_0000_0000_0015L,
            isActive = true
        ).let { op ->
            AluVisualizer(operation = op)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun AluVisualizerIdlePreview() {
    CalcoreTheme {
        AluVisualizer(operation = null)
    }
}
