package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.cloveriris.calcore.ui.theme.TerminalGray

/**
 * 内存网格可视化
 *
 * @param cells 内存单元列表
 * @param columns 每行显示的单元数
 */
data class MemoryCellVisual(
    val address: String,
    val value: Byte,
    val isAllocated: Boolean = true,
    val isPointer: Boolean = false
)

@Composable
fun MemoryGrid(
    cells: List<MemoryCellVisual>,
    modifier: Modifier = Modifier,
    columns: Int = 8
) {
    val textMeasurer = rememberTextMeasurer()
    val rows = (cells.size + columns - 1) / columns

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((rows * 32 + 24).dp)
            .padding(8.dp)
    ) {
        val cellWidth = size.width / columns
        val cellHeight = 28.dp.toPx()
        val gap = 2.dp.toPx()

        cells.forEachIndexed { index, cell ->
            val row = index / columns
            val col = index % columns
            val x = col * cellWidth + gap / 2
            val y = row * cellHeight + gap / 2 + 20.dp.toPx() // 顶部留地址标签空间

            val color = when {
                cell.isPointer -> Color.Transparent // 空心方块
                cell.isAllocated -> TerminalGreen
                else -> Color(0xFF1F1F1F)
            }

            // 绘制方块
            drawRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(cellWidth - gap, cellHeight - gap)
            )

            // 空心方块边框
            if (cell.isPointer) {
                drawRect(
                    color = TerminalGreen,
                    topLeft = Offset(x, y),
                    size = Size(cellWidth - gap, cellHeight - gap),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            // 数值文字
            val valueText = cell.value.toString(16).uppercase().padStart(2, '0')
            val textColor = when {
                cell.isPointer -> TerminalGreen
                cell.isAllocated -> Color.Black
                else -> TerminalGray
            }

            val valueLayout = textMeasurer.measure(
                valueText,
                TextStyle(
                    color = textColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                valueLayout,
                topLeft = Offset(
                    x + (cellWidth - gap - valueLayout.size.width) / 2,
                    y + (cellHeight - gap - valueLayout.size.height) / 2
                )
            )

            // 地址标签（每行第一个）
            if (col == 0) {
                val addrLayout = textMeasurer.measure(
                    cell.address,
                    TextStyle(
                        color = TerminalGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                drawText(
                    addrLayout,
                    topLeft = Offset(x, y - 14.dp.toPx())
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun MemoryGridPreview() {
    CalcoreTheme {
        val cells = List(32) { i ->
            MemoryCellVisual(
                address = "0x%04X".format(i * 8),
                value = (i * 17 % 256).toByte(),
                isAllocated = i % 5 != 0,
                isPointer = i % 7 == 0 && i != 0
            )
        }
        MemoryGrid(cells = cells)
    }
}
