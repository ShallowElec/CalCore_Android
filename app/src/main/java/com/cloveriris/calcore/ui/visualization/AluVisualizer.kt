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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.cloveriris.calcore.ui.theme.LocalVisualizationColors

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
    val viz = LocalVisualizationColors.current
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
            .background(viz.stageBg)
            .padding(8.dp)
    ) {
        Text(
            text = "L3: ALU",
            color = viz.textMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            if (operation == null) {
                val idleLayout = textMeasurer.measure(
                    "ALU IDLE",
                    TextStyle(
                        color = viz.textMuted,
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
            val boxHeight = 40.dp.toPx()
            val opGap = 16.dp.toPx()
            val totalWidth = boxWidth * 3 + opGap * 4
            val startX = (size.width - totalWidth) / 2
            val centerY = 30.dp.toPx()

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
                    color = viz.accent.copy(alpha = flickerAlpha * 0.12f),
                    topLeft = Offset(startX - opGap / 2, centerY - boxHeight / 2 - 4.dp.toPx()),
                    size = Size(totalWidth + opGap, boxHeight + 8.dp.toPx())
                )
                drawRect(
                    color = viz.accent.copy(alpha = flickerAlpha * 0.5f),
                    topLeft = Offset(startX - opGap / 2, centerY - boxHeight / 2 - 4.dp.toPx()),
                    size = Size(totalWidth + opGap, boxHeight + 8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }

            // 左操作数
            drawHexBox(startX, centerY - boxHeight / 2, boxWidth, boxHeight, leftHex, textMeasurer, viz)
            // 运算符
            drawAluOperator(startX + boxWidth + opGap / 2, centerY, opSymbol, textMeasurer, viz)
            // 右操作数
            drawHexBox(startX + boxWidth + opGap, centerY - boxHeight / 2, boxWidth, boxHeight, rightHex, textMeasurer, viz)
            // 等号
            drawAluOperator(startX + boxWidth * 2 + opGap * 1.5f, centerY, "=", textMeasurer, viz)
            // 结果
            drawHexBox(startX + boxWidth * 2 + opGap * 2, centerY - boxHeight / 2, boxWidth, boxHeight, resultHex, textMeasurer, viz, isResult = true)

            // ===== 位运算细节条（底部） =====
            if (operation.isActive) {
                val detailY = centerY + boxHeight / 2 + 8.dp.toPx()
                when (operation.operation) {
                    "ADD", "SUB" -> drawCarryRippleDetail(
                        startX, detailY, boxWidth, operation.left, operation.right, operation.result,
                        textMeasurer, flickerAlpha, viz
                    )
                    "AND", "OR", "XOR" -> drawBitwiseGateDetail(
                        startX, detailY, boxWidth, opGap, operation.operation,
                        operation.left, operation.right, operation.result, textMeasurer, viz
                    )
                    else -> { /* 其他运算不展示细节 */ }
                }
            }
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
    viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme,
    isResult: Boolean = false
) {
    drawRect(
        color = if (isResult) viz.dataPrimary.copy(alpha = 0.08f) else viz.surface,
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    drawRect(
        color = if (isResult) viz.dataPrimary.copy(alpha = 0.4f) else viz.accent.copy(alpha = 0.3f),
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
    val textLayout = textMeasurer.measure(
        hex,
        TextStyle(
            color = viz.dataPrimary,
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
    textMeasurer: TextMeasurer,
    viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme
) {
    val textLayout = textMeasurer.measure(
        symbol,
        TextStyle(
            color = viz.textMuted,
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

/**
 * ADD/SUB 进位传播细节：展示最低 4 位的逐位运算与进位链
 */
private fun DrawScope.drawCarryRippleDetail(
    startX: Float, y: Float, boxWidth: Float,
    left: Long, right: Long, result: Long,
    textMeasurer: TextMeasurer, flickerAlpha: Float,
    viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme
) {
    val bitCount = 4
    val bitW = boxWidth / bitCount
    val bitH = 14.dp.toPx()
    val gap = 2.dp.toPx()

    // 提取最低 4 位
    val leftBits = List(bitCount) { i -> ((left shr i) and 1) == 1L }
    val rightBits = List(bitCount) { i -> ((right shr i) and 1) == 1L }
    val resultBits = List(bitCount) { i -> ((result shr i) and 1) == 1L }

    // 绘制每一位的运算框
    for (i in 0 until bitCount) {
        val bx = startX + (bitCount - 1 - i) * bitW
        val bitIndex = i // 从 LSB 开始

        // 左操作数位
        drawBitCell(bx, y, bitW - gap, bitH, leftBits[bitIndex], textMeasurer, viz, isTop = true)
        // 右操作数位
        drawBitCell(bx, y + bitH + 2.dp.toPx(), bitW - gap, bitH, rightBits[bitIndex], textMeasurer, viz, isTop = false)
        // 结果位
        drawBitCell(bx, y + bitH * 2 + 6.dp.toPx(), bitW - gap, bitH, resultBits[bitIndex], textMeasurer, viz, isResult = true)

        // 进位连接线（相邻位之间）
        if (i < bitCount - 1) {
            val lineY = y + bitH * 2 + 6.dp.toPx() + bitH / 2
            val fromX = bx + bitW / 2
            val toX = bx + bitW + gap / 2
            drawLine(
                color = viz.dataPrimary.copy(alpha = 0.2f + 0.3f * flickerAlpha),
                start = Offset(fromX, lineY),
                end = Offset(toX, lineY),
                strokeWidth = 1.dp.toPx()
            )
            // 进位箭头（从低位指向高位，即从左到右）
            drawLine(
                color = viz.accent.copy(alpha = 0.4f * flickerAlpha),
                start = Offset(fromX + 4.dp.toPx(), lineY - 2.dp.toPx()),
                end = Offset(fromX + 8.dp.toPx(), lineY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = viz.accent.copy(alpha = 0.4f * flickerAlpha),
                start = Offset(fromX + 4.dp.toPx(), lineY + 2.dp.toPx()),
                end = Offset(fromX + 8.dp.toPx(), lineY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }

    // "CARRY RIPPLE" 标签
    val labelLayout = textMeasurer.measure(
        "CARRY",
        TextStyle(color = viz.textMuted.copy(alpha = 0.4f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
    )
    drawText(labelLayout, topLeft = Offset(startX + boxWidth + 4.dp.toPx(), y + bitH))
}

/**
 * 位运算逻辑门细节：展示 AND/OR/XOR 的逐位逻辑门
 */
private fun DrawScope.drawBitwiseGateDetail(
    startX: Float, y: Float, boxWidth: Float, opGap: Float,
    op: String, left: Long, right: Long, result: Long,
    textMeasurer: TextMeasurer,
    viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme
) {
    val bitCount = 4
    val bitW = boxWidth / bitCount
    val bitH = 14.dp.toPx()
    val gap = 2.dp.toPx()

    val leftBits = List(bitCount) { i -> ((left shr i) and 1) == 1L }
    val rightBits = List(bitCount) { i -> ((right shr i) and 1) == 1L }
    val resultBits = List(bitCount) { i -> ((result shr i) and 1) == 1L }

    for (i in 0 until bitCount) {
        val bx = startX + (bitCount - 1 - i) * bitW
        val bitIndex = i

        // 输入位（上下排列）
        drawBitCell(bx, y, bitW - gap, bitH, leftBits[bitIndex], textMeasurer, viz, isTop = true)
        drawBitCell(bx, y + bitH + 2.dp.toPx(), bitW - gap, bitH, rightBits[bitIndex], textMeasurer, viz, isTop = false)

        // 逻辑门符号（居中）
        val gateSymbol = when (op) {
            "AND" -> "&"
            "OR" -> "|"
            "XOR" -> "^"
            else -> "?"
        }
        val gateLayout = textMeasurer.measure(
            gateSymbol,
            TextStyle(color = viz.textMuted.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
        drawText(
            gateLayout,
            topLeft = Offset(
                bx + (bitW - gap - gateLayout.size.width) / 2,
                y + bitH + 2.dp.toPx() + bitH + 2.dp.toPx()
            )
        )

        // 结果位
        drawBitCell(bx, y + bitH * 2 + 14.dp.toPx(), bitW - gap, bitH, resultBits[bitIndex], textMeasurer, viz, isResult = true)

        // 连线（输入→门→输出）
        val centerX = bx + (bitW - gap) / 2
        drawLine(
            color = viz.dataPrimary.copy(alpha = 0.2f),
            start = Offset(centerX, y + bitH),
            end = Offset(centerX, y + bitH * 2 + 14.dp.toPx()),
            strokeWidth = 0.5f.dp.toPx()
        )
    }
}

private fun DrawScope.drawBitCell(
    x: Float, y: Float, w: Float, h: Float,
    bit: Boolean, textMeasurer: TextMeasurer,
    viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme,
    isTop: Boolean = false, isResult: Boolean = false
) {
    val bgColor = when {
        isResult && bit -> viz.dataPrimary.copy(alpha = 0.6f)
        isResult -> viz.surface
        bit -> viz.dataPrimary.copy(alpha = 0.25f)
        else -> Color(0xFF1A1A1A)
    }
    val borderColor = when {
        isResult && bit -> viz.dataPrimary
        isResult -> viz.textMuted.copy(alpha = 0.2f)
        bit -> viz.dataPrimary.copy(alpha = 0.4f)
        else -> Color(0xFF2A2A2A)
    }
    drawRect(color = bgColor, topLeft = Offset(x, y), size = Size(w, h))
    drawRect(color = borderColor, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 0.5f.dp.toPx()))

    val text = if (bit) "1" else "0"
    val textLayout = textMeasurer.measure(
        text,
        TextStyle(
            color = if (bit && isResult) Color.Black else if (bit) viz.dataPrimary else viz.textMuted.copy(alpha = 0.4f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(
        textLayout,
        topLeft = Offset(x + (w - textLayout.size.width) / 2, y + (h - textLayout.size.height) / 2)
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
