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
import com.cloveriris.calcore.presentation.visualization.StackAnimationState
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.LocalVisualizationColors

/**
 * 栈可视化组件（L4 堆栈搬运过程）
 *
 * 特性：
 * - 栈帧垂直堆叠，支持向下生长（主流架构）
 * - PUSH：绿色实心方块从寄存器区滑入栈顶
 * - POP：方块从栈顶滑出至寄存器/ALU 区
 * - 栈指针 (SP) 箭头随 PUSH/POP 同步移动并高亮
 * - 函数调用多层栈帧叠加展示
 *
 * @param frames 栈帧列表，从底到顶
 * @param spValue 当前栈指针显示值
 * @param spHighlighted SP 是否高亮
 * @param stackAnimation 当前栈动画状态（PUSH/POP 滑入滑出）
 * @param spRegisterName 栈指针寄存器名（如 RSP / SP）
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
    spValue: String = "",
    spHighlighted: Boolean = false,
    stackAnimation: StackAnimationState = StackAnimationState(),
    spRegisterName: String = "SP"
) {
    val viz = LocalVisualizationColors.current
    val textMeasurer = rememberTextMeasurer()
    val animProgress = remember(stackAnimation) {
        Animatable(stackAnimation.progress)
    }
    LaunchedEffect(stackAnimation.progress) {
        animProgress.animateTo(stackAnimation.progress, tween(200))
    }

    // 预估高度：每帧 36dp + 顶部寄存器区 48dp + 底部间隙
    val canvasHeight = ((frames.size + 1) * 36 + 56).dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeight)
            .padding(8.dp)
    ) {
        val rowHeight = 32.dp.toPx()
        val gap = 4.dp.toPx()
        val regAreaHeight = 44.dp.toPx()
        val stackStartY = regAreaHeight + 8.dp.toPx()
        val frameWidth = size.width * 0.72f
        val spArrowX = frameWidth + 16.dp.toPx()

        // 绘制寄存器区背景（PUSH 时方块从此滑出，POP 时方块滑入此处）
        drawRect(
            color = viz.stageBg,
            topLeft = Offset(0f, 0f),
            size = Size(frameWidth, regAreaHeight)
        )
        drawRect(
            color = viz.textMuted.copy(alpha = 0.3f),
            topLeft = Offset(0f, 0f),
            size = Size(frameWidth, regAreaHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        val regLabel = textMeasurer.measure(
            "REG / ALU",
            TextStyle(color = viz.textMuted.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
        drawText(regLabel, topLeft = Offset(4.dp.toPx(), 2.dp.toPx()))

        // 绘制 PUSH 动画滑块（从寄存器区滑向栈顶）
        if (stackAnimation.operation == AnimationAction.StackOperationType.PUSH && frames.isNotEmpty()) {
            val topFrameIndex = 0 // 栈顶在 reversed 后是 0
            val targetY = stackStartY + topFrameIndex * (rowHeight + gap)
            val currentY = regAreaHeight - rowHeight + (targetY - (regAreaHeight - rowHeight)) * animProgress.value
            val alpha = 0.3f + 0.7f * animProgress.value

            // 发光拖尾残影（3 个渐淡影子）
            val trailSteps = 3
            for (t in 1..trailSteps) {
                val trailY = currentY - t * (rowHeight * 0.4f)
                if (trailY > regAreaHeight - rowHeight) {
                    val trailAlpha = alpha * (1f - t / (trailSteps + 1f)) * 0.25f
                    drawRect(
                        color = viz.dataPrimary.copy(alpha = trailAlpha),
                        topLeft = Offset(0f, trailY),
                        size = Size(frameWidth, rowHeight)
                    )
                }
            }

            drawStackBlock(
                x = 0f, y = currentY,
                width = frameWidth, height = rowHeight,
                label = stackAnimation.frameLabel,
                value = stackAnimation.frameValue,
                isTop = true,
                alpha = alpha,
                textMeasurer = textMeasurer,
                viz = viz
            )
        }

        // 绘制 POP 动画滑块（从栈顶滑向寄存器区）
        if (stackAnimation.operation == AnimationAction.StackOperationType.POP && frames.isNotEmpty()) {
            val topFrameIndex = 0
            val startY = stackStartY + topFrameIndex * (rowHeight + gap)
            val targetY = regAreaHeight - rowHeight
            val currentY = startY + (targetY - startY) * animProgress.value
            val alpha = 1.0f - 0.7f * animProgress.value

            // 发光拖尾残影
            val trailSteps = 3
            for (t in 1..trailSteps) {
                val trailY = currentY + t * (rowHeight * 0.4f)
                if (trailY < startY + rowHeight) {
                    val trailAlpha = alpha * (1f - t / (trailSteps + 1f)) * 0.25f
                    drawRect(
                        color = viz.dataPrimary.copy(alpha = trailAlpha),
                        topLeft = Offset(0f, trailY),
                        size = Size(frameWidth, rowHeight)
                    )
                }
            }

            drawStackBlock(
                x = 0f, y = currentY,
                width = frameWidth, height = rowHeight,
                label = stackAnimation.frameLabel,
                value = stackAnimation.frameValue,
                isTop = true,
                alpha = alpha,
                textMeasurer = textMeasurer,
                viz = viz
            )
        }

        // 绘制稳定栈帧（从底到顶，反转后从上到下绘制）
        val visibleFrames = when (stackAnimation.operation) {
            AnimationAction.StackOperationType.PUSH -> if (animProgress.value > 0.9f) frames else frames.dropLast(1)
            AnimationAction.StackOperationType.POP -> if (animProgress.value > 0.1f) frames else frames.dropLast(1)
            else -> frames
        }

        visibleFrames.reversed().forEachIndexed { index, frame ->
            val y = stackStartY + index * (rowHeight + gap)
            val isTop = index == 0
            drawStackBlock(
                x = 0f, y = y,
                width = frameWidth, height = rowHeight,
                label = frame.label,
                value = frame.value,
                isTop = isTop,
                alpha = 1f,
                textMeasurer = textMeasurer,
                viz = viz
            )
        }

        // 绘制栈指针箭头与标签
        val spY = if (frames.isEmpty()) {
            stackStartY
        } else {
            stackStartY - rowHeight * 0.2f
        }
        val spText = if (spValue.isNotEmpty()) "▼ $spRegisterName = $spValue" else "▼ $spRegisterName"
        val spColor = if (spHighlighted) viz.accent else viz.textMuted
        val spLayout = textMeasurer.measure(
            spText,
            TextStyle(color = spColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )
        drawText(spLayout, topLeft = Offset(spArrowX, spY.coerceAtLeast(0f)))

        // SP 高亮时绘制闪烁箭头
        if (spHighlighted) {
            val arrowPath = androidx.compose.ui.graphics.Path().apply {
                val ax = spArrowX - 10.dp.toPx()
                val ay = spY + spLayout.size.height / 2f
                moveTo(ax, ay - 6.dp.toPx())
                lineTo(ax - 6.dp.toPx(), ay + 4.dp.toPx())
                lineTo(ax + 6.dp.toPx(), ay + 4.dp.toPx())
                close()
            }
            drawPath(path = arrowPath, color = viz.accent.copy(alpha = 0.6f + 0.4f * animProgress.value))
        }
    }
}

private fun DrawScope.drawStackBlock(
    x: Float, y: Float,
    width: Float, height: Float,
    label: String,
    value: String,
    isTop: Boolean,
    alpha: Float,
    textMeasurer: TextMeasurer,
    viz: com.cloveriris.calcore.ui.theme.VisualizationColorScheme
) {
    val bgColor = when {
        isTop -> viz.dataPrimary.copy(alpha = 0.2f * alpha)
        else -> viz.surface.copy(alpha = alpha)
    }
    val borderColor = when {
        isTop -> viz.dataPrimary.copy(alpha = 0.9f * alpha)
        else -> viz.textMuted.copy(alpha = 0.3f * alpha)
    }

    // 背景
    drawRect(
        color = bgColor,
        topLeft = Offset(x, y),
        size = Size(width, height)
    )

    // 边框（绿色实心 = 已分配数据）
    drawRect(
        color = borderColor,
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isTop) 2.dp.toPx() else 1.dp.toPx())
    )

    // 标签文字
    val labelLayout = textMeasurer.measure(
        label,
        TextStyle(
            color = if (isTop) viz.dataPrimary.copy(alpha = alpha) else viz.textMuted.copy(alpha = 0.8f * alpha),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(
        labelLayout,
        topLeft = Offset(x + 8.dp.toPx(), y + (height - labelLayout.size.height) / 2)
    )

    // 值文字
    val valueLayout = textMeasurer.measure(
        value,
        TextStyle(
            color = if (isTop) viz.dataPrimary.copy(alpha = alpha) else viz.textMuted.copy(alpha = 0.6f * alpha),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(
        valueLayout,
        topLeft = Offset(x + width * 0.45f, y + (height - valueLayout.size.height) / 2)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun StackViewPreview() {
    CalcoreTheme {
        StackView(
            frames = listOf(
                StackFrameVisual("main()", "0x7FFE_EFBC9000"),
                StackFrameVisual("fact(3)", "0x7FFE_EFBC8FE0", isActive = true),
                StackFrameVisual("fact(2)", "0x7FFE_EFBC8FC0", isActive = true),
                StackFrameVisual("fact(1)", "0x7FFE_EFBC8FA0", isActive = true)
            ),
            spValue = "0x7FFE_EFBC8F80",
            spHighlighted = true,
            stackAnimation = StackAnimationState(
                operation = AnimationAction.StackOperationType.PUSH,
                progress = 0.6f,
                frameLabel = "fact(0)",
                frameValue = "0x7FFE_EFBC8F80",
                registerName = "RSP"
            ),
            spRegisterName = "RSP"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun StackViewEmptyPreview() {
    CalcoreTheme {
        StackView(
            frames = emptyList(),
            spValue = "0x7FFE_EFBC9000",
            spRegisterName = "RSP"
        )
    }
}
