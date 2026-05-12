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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.cloveriris.calcore.presentation.visualization.MemoryPointerAnimationState
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalAmber
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.theme.TerminalGray

/**
 * 内存网格可视化（L4 内存布局 + L6 指针寻址）
 *
 * 特性：
 * - 绿色实心方块：已分配的内存单元 / 实际数据
 * - 绿色空心方块：指针、引用、空闲槽位、预分配但未写入的地址
 * - 光标读取头：黄色高亮边框 + 顶部箭头，随光标移动平滑过渡
 * - 指针连线生长：指针赋值时虚线箭头从源地址“生长”到目标地址
 * - 写入脉冲：正在写入的单元格闪烁白色
 */
data class MemoryCellVisual(
    val address: String,
    val value: Byte,
    val isAllocated: Boolean = true,
    val isPointer: Boolean = false,
    val isWriting: Boolean = false
)

@Composable
fun MemoryGrid(
    cells: List<MemoryCellVisual>,
    modifier: Modifier = Modifier,
    columns: Int = 8,
    cursorAddress: Int = -1,
    previousCursorAddress: Int = -1,
    pointerLinks: List<Pair<Int, Int>> = emptyList(),
    pointerAnimation: MemoryPointerAnimationState = MemoryPointerAnimationState()
) {
    val textMeasurer = rememberTextMeasurer()
    val rows = (cells.size + columns - 1) / columns

    // 光标平滑移动动画
    val cursorAnim = remember(cursorAddress) { Animatable(0f) }
    LaunchedEffect(cursorAddress) {
        cursorAnim.snapTo(0f)
        cursorAnim.animateTo(1f, tween(250))
    }

    // 写入脉冲动画
    val writePulse = remember(cells.filter { it.isWriting }.hashCode()) { Animatable(0f) }
    LaunchedEffect(cells.filter { it.isWriting }.hashCode()) {
        writePulse.snapTo(0f)
        writePulse.animateTo(1f, tween(400))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((rows * 32 + 24).dp)
            .padding(8.dp)
    ) {
        val cellWidth = size.width / columns
        val cellHeight = 28.dp.toPx()
        val gap = 2.dp.toPx()

        // 计算当前光标索引
        val cursorIndex = resolveCursorIndex(cells, cursorAddress)
        val prevCursorIndex = resolveCursorIndex(cells, previousCursorAddress)

        cells.forEachIndexed { index, cell ->
            val row = index / columns
            val col = index % columns
            val x = col * cellWidth + gap / 2
            val y = row * cellHeight + gap / 2 + 20.dp.toPx()

            val isCursor = index == cursorIndex
            val wasCursor = index == prevCursorIndex && prevCursorIndex != cursorIndex

            // 光标平滑移动：若此单元格是前一个光标位置，随动画淡出；若是当前位置，随动画淡入
            val cursorAlpha = when {
                isCursor -> cursorAnim.value
                wasCursor -> 1f - cursorAnim.value
                else -> if (index == cursorIndex) 1f else 0f
            }

            // 方块颜色语义
            val fillColor = when {
                cell.isPointer -> Color.Transparent // 空心 = 指针/空闲
                cell.isAllocated -> TerminalGreen   // 实心 = 已分配数据
                else -> Color(0xFF1F1F1F)
            }

            // 绘制方块
            drawRect(
                color = fillColor,
                topLeft = Offset(x, y),
                size = Size(cellWidth - gap, cellHeight - gap)
            )

            // 空心方块边框（指针语义）
            if (cell.isPointer) {
                drawRect(
                    color = TerminalGreen,
                    topLeft = Offset(x, y),
                    size = Size(cellWidth - gap, cellHeight - gap),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 写入脉冲效果（白色闪烁边框）
            if (cell.isWriting) {
                val pulseAlpha = (1f - writePulse.value) * 0.8f
                drawRect(
                    color = Color.White.copy(alpha = pulseAlpha),
                    topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                    size = Size(cellWidth - gap + 2.dp.toPx(), cellHeight - gap + 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
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
                TextStyle(color = textColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
                    TextStyle(color = TerminalGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                )
                drawText(addrLayout, topLeft = Offset(x, y - 14.dp.toPx()))
            }

            // 光标读取头（黄色外边框 + 顶部小三角）
            if (cursorAlpha > 0.01f) {
                val strokeWidth = 2.dp.toPx()
                drawRect(
                    color = TerminalAmber.copy(alpha = cursorAlpha),
                    topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                    size = Size(cellWidth - gap + 2.dp.toPx(), cellHeight - gap + 2.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                val triCenterX = x + (cellWidth - gap) / 2
                val triTop = y - 10.dp.toPx()
                val triTip = y - 2.dp.toPx()
                val halfW = 5.dp.toPx()
                val triangle = Path().apply {
                    moveTo(triCenterX, triTip)
                    lineTo(triCenterX - halfW, triTop)
                    lineTo(triCenterX + halfW, triTop)
                    close()
                }
                drawPath(path = triangle, color = TerminalAmber.copy(alpha = cursorAlpha))
            }
        }

        // 静态指针虚线箭头
        pointerLinks.forEach { (sourceIndex, targetIndex) ->
            if (sourceIndex !in cells.indices || targetIndex !in cells.indices) return@forEach
            drawPointerArrow(sourceIndex, targetIndex, columns, cellWidth, cellHeight, gap, TerminalGreen.copy(alpha = 0.5f))
        }

        // 动态指针连线生长动画
        if (pointerAnimation.progress > 0f) {
            val sIdx = cells.indexOfFirst {
                it.address.removePrefix("0x").removePrefix("0X").toIntOrNull(16) == pointerAnimation.sourceAddress
            }
            val tIdx = cells.indexOfFirst {
                it.address.removePrefix("0x").removePrefix("0X").toIntOrNull(16) == pointerAnimation.targetAddress
            }
            if (sIdx >= 0 && tIdx >= 0) {
                drawGrowingPointerArrow(
                    sIdx, tIdx, columns, cellWidth, cellHeight, gap,
                    TerminalGreen, pointerAnimation.progress
                )
            }
        }
    }
}

private fun resolveCursorIndex(cells: List<MemoryCellVisual>, cursorAddress: Int): Int {
    return when {
        cursorAddress < 0 -> -1
        else -> {
            val byAddress = cells.indexOfFirst { cell ->
                cell.address.removePrefix("0x").removePrefix("0X")
                    .toIntOrNull(16) == cursorAddress
            }
            if (byAddress >= 0) byAddress else if (cursorAddress < cells.size) cursorAddress else -1
        }
    }
}

private fun DrawScope.drawPointerArrow(
    sourceIndex: Int, targetIndex: Int,
    columns: Int, cellWidth: Float, cellHeight: Float, gap: Float,
    color: Color
) {
    val sourceRow = sourceIndex / columns
    val sourceCol = sourceIndex % columns
    val targetRow = targetIndex / columns
    val targetCol = targetIndex % columns

    val sourceX = sourceCol * cellWidth + cellWidth / 2
    val sourceY = sourceRow * cellHeight + 20.dp.toPx() + (cellHeight - gap) / 2
    val targetX = targetCol * cellWidth + cellWidth / 2
    val targetY = targetRow * cellHeight + 20.dp.toPx() + (cellHeight - gap) / 2

    val start = Offset(sourceX, sourceY)
    val end = Offset(targetX, targetY)

    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    )

    val dx = targetX - sourceX
    val dy = targetY - sourceY
    val len = kotlin.math.hypot(dx, dy)
    if (len > 0.001f) {
        val ux = dx / len
        val uy = dy / len
        val arrowLen = 6.dp.toPx()
        val arrowWidth = 4.dp.toPx()
        val baseX = targetX - ux * arrowLen
        val baseY = targetY - uy * arrowLen
        val perpX = -uy * arrowWidth
        val perpY = ux * arrowWidth
        val arrowPath = Path().apply {
            moveTo(targetX, targetY)
            lineTo(baseX + perpX, baseY + perpY)
            lineTo(baseX - perpX, baseY - perpY)
            close()
        }
        drawPath(path = arrowPath, color = color)
    }
}

private fun DrawScope.drawGrowingPointerArrow(
    sourceIndex: Int, targetIndex: Int,
    columns: Int, cellWidth: Float, cellHeight: Float, gap: Float,
    color: Color, progress: Float
) {
    val sourceRow = sourceIndex / columns
    val sourceCol = sourceIndex % columns
    val targetRow = targetIndex / columns
    val targetCol = targetIndex % columns

    val sourceX = sourceCol * cellWidth + cellWidth / 2
    val sourceY = sourceRow * cellHeight + 20.dp.toPx() + (cellHeight - gap) / 2
    val targetX = targetCol * cellWidth + cellWidth / 2
    val targetY = targetRow * cellHeight + 20.dp.toPx() + (cellHeight - gap) / 2

    val start = Offset(sourceX, sourceY)
    val end = Offset(
        sourceX + (targetX - sourceX) * progress,
        sourceY + (targetY - sourceY) * progress
    )

    // 生长中的实线
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 2.dp.toPx()
    )

    // 流光点
    if (progress > 0.3f) {
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 3.dp.toPx(),
            center = end
        )
    }

    // 箭头头部
    if (progress > 0.9f) {
        val dx = targetX - sourceX
        val dy = targetY - sourceY
        val len = kotlin.math.hypot(dx, dy)
        if (len > 0.001f) {
            val ux = dx / len
            val uy = dy / len
            val arrowLen = 6.dp.toPx()
            val arrowWidth = 4.dp.toPx()
            val baseX = targetX - ux * arrowLen
            val baseY = targetY - uy * arrowLen
            val perpX = -uy * arrowWidth
            val perpY = ux * arrowWidth
            val arrowPath = Path().apply {
                moveTo(targetX, targetY)
                lineTo(baseX + perpX, baseY + perpY)
                lineTo(baseX - perpX, baseY - perpY)
                close()
            }
            drawPath(path = arrowPath, color = color)
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
                isPointer = i % 7 == 0 && i != 0,
                isWriting = i == 3
            )
        }
        MemoryGrid(
            cells = cells,
            cursorAddress = 16,
            previousCursorAddress = 8,
            pointerLinks = listOf(2 to 5, 10 to 14),
            pointerAnimation = MemoryPointerAnimationState(2, 5, progress = 0.7f)
        )
    }
}
