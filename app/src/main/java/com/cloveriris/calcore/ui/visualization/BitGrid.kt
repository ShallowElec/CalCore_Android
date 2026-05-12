package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.SkyBlue
import com.cloveriris.calcore.ui.theme.TerminalAmber
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.theme.TerminalGray

/**
 * 64-bit 位格可视化组件
 *
 * @param bits 64 个布尔值，表示每一位的状态
 * @param labels 可选的位标签（如符号位、指数位、尾数位）
 * @param layout 布局方式：8×8 网格或 1×64 横向条
 * @param highlights 高亮的 bit 索引集合（L2）
 * @param label 位格标签，用于触发 IEEE 754 分区标注
 */
enum class BitGridLayout { GRID_8X8, BAR_1X64 }

@Composable
fun BitGrid(
    bits: List<Boolean>,
    modifier: Modifier = Modifier,
    layout: BitGridLayout = BitGridLayout.GRID_8X8,
    labels: List<String>? = null,
    highlights: Set<Int> = emptySet(),
    label: String = ""
) {
    val textMeasurer = rememberTextMeasurer()

    // 位翻转动画状态：每个索引对应一个 Animatable（0f=正常, 1f=翻转峰值）
    val flipAnims = remember { mutableStateMapOf<Int, Animatable<Float, *>>() }
    val prevBits = remember(bits) { bits.toList() }

    // 检测位翻转并触发动画（波浪式延迟）
    LaunchedEffect(bits) {
        val flipped = bits.indices.filter { i ->
            i < prevBits.size && bits[i] != prevBits[i]
        }
        flipped.forEachIndexed { waveIndex, bitIndex ->
            val anim = flipAnims.getOrPut(bitIndex) { Animatable(0f) }
            anim.snapTo(0f)
            // 波浪延迟：从左到右（或从高位到低位）依次翻转
            val delayMs = waveIndex * 18L
            kotlinx.coroutines.delay(delayMs)
            anim.animateTo(1f, tween(180))
            anim.animateTo(0f, tween(220))
        }
    }

    Canvas(
        modifier = when (layout) {
            BitGridLayout.GRID_8X8 -> modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
            BitGridLayout.BAR_1X64 -> modifier
                .fillMaxWidth()
                .aspectRatio(8f)
                .padding(8.dp)
        }
    ) {
        when (layout) {
            BitGridLayout.GRID_8X8 -> drawBitGrid8x8(bits, labels, textMeasurer, highlights, flipAnims)
            BitGridLayout.BAR_1X64 -> drawBitGrid1x64(bits, labels, textMeasurer, highlights, label, flipAnims)
        }
    }
}

private fun DrawScope.drawBitGrid8x8(
    bits: List<Boolean>,
    labels: List<String>?,
    textMeasurer: TextMeasurer,
    highlights: Set<Int>,
    flipAnims: Map<Int, Animatable<Float, *>>
) {
    val cellSize = size.width / 8f
    val gap = 2.dp.toPx()
    val actualCell = cellSize - gap

    for (i in 0 until 64) {
        val row = i / 8
        val col = i % 8
        val x = col * cellSize + gap / 2
        val y = row * cellSize + gap / 2
        val bit = bits.getOrElse(i) { false }
        val flipProgress = flipAnims[i]?.value ?: 0f

        // 翻转时的缩放和发光
        val scale = 1f + flipProgress * 0.35f
        val glowAlpha = flipProgress * 0.6f
        val cellCenterX = x + actualCell / 2
        val cellCenterY = y + actualCell / 2
        val scaledCell = actualCell * scale
        val sx = cellCenterX - scaledCell / 2
        val sy = cellCenterY - scaledCell / 2

        // 发光背景（翻转时白色闪光）
        if (flipProgress > 0.01f) {
            drawRect(
                color = Color.White.copy(alpha = glowAlpha),
                topLeft = Offset(sx - 2.dp.toPx(), sy - 2.dp.toPx()),
                size = Size(scaledCell + 4.dp.toPx(), scaledCell + 4.dp.toPx())
            )
        }

        drawRect(
            color = if (bit) TerminalGreen else Color(0xFF1F1F1F),
            topLeft = Offset(sx, sy),
            size = Size(scaledCell, scaledCell)
        )

        // 绘制位值（0/1）
        val bitText = if (bit) "1" else "0"
        val textLayout = textMeasurer.measure(
            bitText,
            TextStyle(
                color = if (bit) Color.Black else Color(0xFF8B949E),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        )
        drawText(
            textLayout,
            topLeft = Offset(
                cellCenterX - textLayout.size.width / 2,
                cellCenterY - textLayout.size.height / 2
            )
        )

        // L2 高亮边框（琥珀色 2dp）
        if (i in highlights) {
            drawRect(
                color = TerminalAmber,
                topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                size = Size(actualCell + 2.dp.toPx(), actualCell + 2.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 翻转时的脉冲边框
        if (flipProgress > 0.01f) {
            drawRect(
                color = TerminalGreen.copy(alpha = flipProgress * 0.8f),
                topLeft = Offset(sx - 1.dp.toPx(), sy - 1.dp.toPx()),
                size = Size(scaledCell + 2.dp.toPx(), scaledCell + 2.dp.toPx()),
                style = Stroke(width = 1.5f.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawBitGrid1x64(
    bits: List<Boolean>,
    labels: List<String>?,
    textMeasurer: TextMeasurer,
    highlights: Set<Int>,
    label: String,
    flipAnims: Map<Int, Animatable<Float, *>>
) {
    val showIeee754 = label.contains("IEEE 754", ignoreCase = true) || label.contains("DOUBLE", ignoreCase = true)
    val topPadding = if (showIeee754 || highlights.isNotEmpty()) 18.dp.toPx() else 0f

    val cellWidth = size.width / 64f
    val gap = 1.dp.toPx()
    val actualWidth = cellWidth - gap
    val height = size.height - topPadding

    // IEEE 754 分区标注
    if (showIeee754) {
        drawIeee754Labels(cellWidth, topPadding, textMeasurer)
    }

    for (i in 0 until 64) {
        val x = i * cellWidth + gap / 2
        val y = topPadding
        val bit = bits.getOrElse(i) { false }
        val flipProgress = flipAnims[i]?.value ?: 0f

        val scaleY = 1f + flipProgress * 0.5f
        val glowAlpha = flipProgress * 0.5f
        val cellCenterY = y + height / 2
        val scaledHeight = height * scaleY
        val sy = cellCenterY - scaledHeight / 2

        // 发光背景
        if (flipProgress > 0.01f) {
            drawRect(
                color = Color.White.copy(alpha = glowAlpha),
                topLeft = Offset(x - 1.dp.toPx(), sy - 1.dp.toPx()),
                size = Size(actualWidth + 2.dp.toPx(), scaledHeight + 2.dp.toPx())
            )
        }

        drawRect(
            color = if (bit) TerminalGreen else Color(0xFF1F1F1F),
            topLeft = Offset(x, sy),
            size = Size(actualWidth, scaledHeight)
        )

        // L2 高亮三角形标记（琥珀色）
        if (i in highlights) {
            val triCenterX = x + actualWidth / 2
            val triTop = topPadding - 8.dp.toPx()
            val triTip = topPadding - 1.dp.toPx()
            val halfW = 5.dp.toPx()
            val triangle = Path().apply {
                moveTo(triCenterX, triTip)
                lineTo(triCenterX - halfW, triTop)
                lineTo(triCenterX + halfW, triTop)
                close()
            }
            drawPath(
                path = triangle,
                color = TerminalAmber
            )
        }

        // 翻转时的脉冲边框
        if (flipProgress > 0.01f) {
            drawRect(
                color = TerminalGreen.copy(alpha = flipProgress * 0.7f),
                topLeft = Offset(x - 0.5f.dp.toPx(), sy - 0.5f.dp.toPx()),
                size = Size(actualWidth + 1.dp.toPx(), scaledHeight + 1.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawIeee754Labels(
    cellWidth: Float,
    topPadding: Float,
    textMeasurer: TextMeasurer
) {
    // Sign: bit 0 (1 bit)
    val signCenter = cellWidth * 0.5f
    val signLayout = textMeasurer.measure(
        "S",
        TextStyle(color = TerminalAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    )
    drawText(signLayout, topLeft = Offset(signCenter - signLayout.size.width / 2, 0f))

    // Exponent: bits 1..11 (11 bits)
    val expCenter = cellWidth * 1 + cellWidth * 11 / 2f
    val expLayout = textMeasurer.measure(
        "E×11",
        TextStyle(color = SkyBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    )
    drawText(expLayout, topLeft = Offset(expCenter - expLayout.size.width / 2, 0f))

    // Mantissa: bits 12..63 (52 bits)
    val mantCenter = cellWidth * 12 + cellWidth * 52 / 2f
    val mantLayout = textMeasurer.measure(
        "M×52",
        TextStyle(color = TerminalGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    )
    drawText(mantLayout, topLeft = Offset(mantCenter - mantLayout.size.width / 2, 0f))

    // 底部分隔线提示
    drawLine(
        color = TerminalAmber.copy(alpha = 0.5f),
        start = Offset(cellWidth, topPadding - 2.dp.toPx()),
        end = Offset(cellWidth, topPadding),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = SkyBlue.copy(alpha = 0.5f),
        start = Offset(cellWidth * 12, topPadding - 2.dp.toPx()),
        end = Offset(cellWidth * 12, topPadding),
        strokeWidth = 1.dp.toPx()
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun BitGridPreview() {
    CalcoreTheme {
        val bits = remember {
            List(64) { i -> i % 3 == 0 || i == 63 }
        }
        BitGrid(bits = bits)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun BitGridBarPreview() {
    CalcoreTheme {
        val bits = remember {
            List(64) { i -> i % 5 == 0 || i > 60 }
        }
        BitGrid(
            bits = bits,
            layout = BitGridLayout.BAR_1X64,
            highlights = setOf(0, 11, 12, 63),
            label = "IEEE 754 DOUBLE"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun BitGridHighlightsPreview() {
    CalcoreTheme {
        val bits = remember {
            List(64) { i -> i % 4 == 0 }
        }
        BitGrid(
            bits = bits,
            layout = BitGridLayout.GRID_8X8,
            highlights = setOf(0, 1, 2, 63)
        )
    }
}
