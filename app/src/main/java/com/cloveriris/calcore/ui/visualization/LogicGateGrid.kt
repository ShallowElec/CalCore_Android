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
import androidx.compose.ui.graphics.Brush
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
import com.cloveriris.calcore.domain.model.AnimationAction
import com.cloveriris.calcore.presentation.visualization.LogicGateState
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.LocalVisualizationColors

/**
 * 逻辑门网格可视化（L1: Boolean Algebra）
 *
 * @param state 逻辑门状态，包含门类型、输入/输出位和信号流动进度
 */
@Composable
fun LogicGateGrid(
    state: LogicGateState,
    modifier: Modifier = Modifier
) {
    val viz = LocalVisualizationColors.current
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "gate-flicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
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
            text = "L1: BOOLEAN ALGEBRA",
            color = viz.textMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val bitCount = 8
            val bitSize = 14.dp.toPx()
            val bitGap = 4.dp.toPx()
            val colGap = 40.dp.toPx()
            val gateWidth = 84.dp.toPx()
            val gateHeight = (bitCount * (bitSize + bitGap) - bitGap).coerceAtLeast(gateWidth * 0.6f)
            val leftX = 8.dp.toPx()
            val rightX = size.width - 8.dp.toPx() - bitSize
            val gateX = (size.width - gateWidth) / 2
            val gateY = (size.height - gateHeight) / 2

            val gateColor = when (state.gateType) {
                AnimationAction.LogicGateType.AND -> viz.dataPrimary
                AnimationAction.LogicGateType.OR -> viz.accent
                AnimationAction.LogicGateType.XOR -> viz.dataSecondary
                AnimationAction.LogicGateType.NOT -> viz.textMuted
            }

            // 输入位（左）
            for (i in 0 until bitCount) {
                val bit = state.leftBits.getOrElse(i) { false }
                val y = gateY + i * (bitSize + bitGap)
                drawBitSquare(leftX, y, bitSize, bit, viz)
            }

            // 输出位（右）
            for (i in 0 until bitCount) {
                val bit = state.resultBits.getOrElse(i) { false }
                val y = gateY + i * (bitSize + bitGap)
                drawBitSquare(rightX, y, bitSize, bit, viz)
            }

            // 逻辑门中央大方框
            val gateBgAlpha = if (state.signalProgress > 0.5f) flickerAlpha * 0.35f else 0.08f
            drawRect(
                color = gateColor.copy(alpha = gateBgAlpha),
                topLeft = Offset(gateX, gateY),
                size = Size(gateWidth, gateHeight)
            )
            drawRect(
                color = gateColor,
                topLeft = Offset(gateX, gateY),
                size = Size(gateWidth, gateHeight),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // 门类型文字
            val gateText = state.gateType.name
            val gateTextLayout = textMeasurer.measure(
                gateText,
                TextStyle(
                    color = gateColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                gateTextLayout,
                topLeft = Offset(
                    gateX + (gateWidth - gateTextLayout.size.width) / 2,
                    gateY + (gateHeight - gateTextLayout.size.height) / 2
                )
            )

            // 信号流动画：每个 bit 一条连线
            for (i in 0 until bitCount) {
                val bitY = gateY + i * (bitSize + bitGap) + bitSize / 2
                val leftLineStart = Offset(leftX + bitSize, bitY)
                val leftLineEnd = Offset(gateX, bitY)
                val rightLineStart = Offset(gateX + gateWidth, bitY)
                val rightLineEnd = Offset(rightX, bitY)

                // 基线（暗色）
                drawLine(
                    color = viz.dataPrimary.copy(alpha = 0.12f),
                    start = leftLineStart,
                    end = leftLineEnd,
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = viz.dataPrimary.copy(alpha = 0.12f),
                    start = rightLineStart,
                    end = rightLineEnd,
                    strokeWidth = 1.dp.toPx()
                )

                val progress = state.signalProgress
                if (progress > 0f) {
                    // 左输入 → 逻辑门（progress 0.0~0.5）
                    val leftProgress = (progress * 2f).coerceIn(0f, 1f)
                    drawFlowLine(leftLineStart, leftLineEnd, leftProgress, viz)

                    // 逻辑门 → 右输出（progress 0.5~1.0）
                    val rightProgress = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
                    drawFlowLine(rightLineStart, rightLineEnd, rightProgress, viz)
                }
            }
        }
    }
}

private fun DrawScope.drawBitSquare(x: Float, y: Float, size: Float, bit: Boolean, viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme) {
    drawRect(
        color = if (bit) viz.dataPrimary else viz.surface,
        topLeft = Offset(x, y),
        size = Size(size, size)
    )
    val bitText = if (bit) "1" else "0"
    // 不在这里绘制文字以保持一致性，保持简洁小方块
}

private fun DrawScope.drawFlowLine(start: Offset, end: Offset, progress: Float, viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme) {
    if (progress <= 0f) return
    val currentX = start.x + (end.x - start.x) * progress
    val currentY = start.y + (end.y - start.y) * progress
    val head = Offset(currentX, currentY)
    val trailLength = 28.dp.toPx()

    val dist = end.x - start.x
    val segStartX = (currentX - trailLength).coerceAtLeast(start.x)
    val segStart = Offset(segStartX, currentY)

    if (segStart.x < head.x) {
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    viz.dataPrimary.copy(alpha = 0.05f),
                    viz.dataPrimary.copy(alpha = 0.9f)
                ),
                start = segStart,
                end = head
            ),
            start = segStart,
            end = head,
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun LogicGateGridPreview() {
    CalcoreTheme {
        LogicGateGrid(
            state = LogicGateState(
                gateType = AnimationAction.LogicGateType.AND,
                leftBits = List(8) { it % 2 == 0 },
                rightBits = List(8) { it % 2 == 1 },
                resultBits = List(8) { it % 3 == 0 },
                signalProgress = 0.75f
            )
        )
    }
}
