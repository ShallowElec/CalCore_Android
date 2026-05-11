package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 64-bit 位格可视化组件
 *
 * @param bits 64 个布尔值，表示每一位的状态
 * @param labels 可选的位标签（如符号位、指数位、尾数位）
 * @param layout 布局方式：8×8 网格或 1×64 横向条
 */
enum class BitGridLayout { GRID_8X8, BAR_1X64 }

@Composable
fun BitGrid(
    bits: List<Boolean>,
    modifier: Modifier = Modifier,
    layout: BitGridLayout = BitGridLayout.GRID_8X8,
    labels: List<String>? = null
) {
    val textMeasurer = rememberTextMeasurer()

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
            BitGridLayout.GRID_8X8 -> drawBitGrid8x8(bits, labels, textMeasurer)
            BitGridLayout.BAR_1X64 -> drawBitGrid1x64(bits, labels, textMeasurer)
        }
    }
}

private fun DrawScope.drawBitGrid8x8(
    bits: List<Boolean>,
    labels: List<String>?,
    textMeasurer: TextMeasurer
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

        drawRect(
            color = if (bit) TerminalGreen else Color(0xFF1F1F1F),
            topLeft = Offset(x, y),
            size = Size(actualCell, actualCell)
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
                x + (actualCell - textLayout.size.width) / 2,
                y + (actualCell - textLayout.size.height) / 2
            )
        )
    }
}

private fun DrawScope.drawBitGrid1x64(
    bits: List<Boolean>,
    labels: List<String>?,
    textMeasurer: TextMeasurer
) {
    val cellWidth = size.width / 64f
    val gap = 1.dp.toPx()
    val actualWidth = cellWidth - gap
    val height = size.height

    for (i in 0 until 64) {
        val x = i * cellWidth + gap / 2
        val bit = bits.getOrElse(i) { false }

        drawRect(
            color = if (bit) TerminalGreen else Color(0xFF1F1F1F),
            topLeft = Offset(x, 0f),
            size = Size(actualWidth, height)
        )
    }
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
