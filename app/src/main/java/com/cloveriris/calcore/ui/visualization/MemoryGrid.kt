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
 * - 固定 8×4 网格（32 单元格），像 hexdump -C 的秩序感
 * - 地址标签左置，等宽对齐
 * - 绿色实心方块：已分配的内存单元 / 实际数据
 * - 绿色空心方块：指针、引用、空闲槽位
 * - 未分配：仅显示极细网格线，无填充
 * - 写入脉冲：白色闪光→渐绿→稳定
 * - 光标读取头：黄色高亮边框 + 顶部箭头，移动时有拖尾
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
    // 固定行数：至少 4 行，形成稳定的内存 dump 视图
    val rows = 4.coerceAtLeast((cells.size + columns - 1) / columns)

    // 光标平滑移动动画
    val cursorAnim = remember(cursorAddress) { Animatable(0f) }
    LaunchedEffect(cursorAddress) {
        cursorAnim.snapTo(0f)
        cursorAnim.animateTo(1f, tween(250))
    }

    // 写入脉冲动画（白色闪光→绿色）
    val writePulse = remember(cells.filter { it.isWriting }.hashCode()) { Animatable(0f) }
    LaunchedEffect(cells.filter { it.isWriting }.hashCode()) {
        writePulse.snapTo(0f)
        writePulse.animateTo(1f, tween(500))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((rows * 32 + 28).dp)
            .padding(8.dp)
    ) {
        val addrColWidth = 52.dp.toPx()
        val cellWidth = (size.width - addrColWidth) / columns
        val cellHeight = 28.dp.toPx()
        val gap = 2.dp.toPx()

        // 绘制网格背景线（所有单元格的基础网格）
        drawBaseGrid(rows, columns, addrColWidth, cellWidth, cellHeight, gap)

        // 计算光标索引
        val cursorIndex = resolveCursorIndex(cells, cursorAddress)
        val prevCursorIndex = resolveCursorIndex(cells, previousCursorAddress)

        cells.forEachIndexed { index, cell ->
            val row = index / columns
            val col = index % columns
            val x = addrColWidth + col * cellWidth + gap / 2
            val y = row * cellHeight + gap / 2 + 18.dp.toPx()

            val isCursor = index == cursorIndex
            val wasCursor = index == prevCursorIndex && prevCursorIndex != cursorIndex

            // 光标平滑移动淡入淡出
            val cursorAlpha = when {
                isCursor -> cursorAnim.value
                wasCursor -> 1f - cursorAnim.value
                else -> if (index == cursorIndex) 1f else 0f
            }

            // 方块填充语义
            val fillColor = when {
                !cell.isAllocated -> Color.Transparent
                cell.isPointer -> Color.Transparent
                else -> TerminalGreen
            }

            // 写入脉冲：先白色闪光，再渐绿
            val writeGlowAlpha = if (cell.isWriting) {
                val p = writePulse.value
                when {
                    p < 0.3f -> 1f - p / 0.3f  // 白色闪光峰值
                    p < 0.7f -> (p - 0.3f) / 0.4f * 0.6f  // 渐绿过渡
                    else -> 0.6f - (p - 0.7f) / 0.3f * 0.6f  // 衰减
                }
            } else 0f

            // 绘制填充
            if (fillColor != Color.Transparent) {
                drawRect(
                    color = if (cell.isWriting && writePulse.value < 0.5f)
                        Color.White.copy(alpha = writeGlowAlpha.coerceIn(0f, 0.9f))
                    else fillColor,
                    topLeft = Offset(x, y),
                    size = Size(cellWidth - gap, cellHeight - gap)
                )
            }

            // 空心方块边框（指针语义）
            if (cell.isPointer) {
                drawRect(
                    color = TerminalGreen,
                    topLeft = Offset(x, y),
                    size = Size(cellWidth - gap, cellHeight - gap),
                    style = Stroke(width = 1.5f.dp.toPx())
                )
            }

            // 写入脉冲边框（绿色发光）
            if (cell.isWriting && writePulse.value > 0.3f) {
                drawRect(
                    color = TerminalGreen.copy(alpha = writeGlowAlpha * 0.8f),
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
                else -> TerminalGray.copy(alpha = 0.3f)
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

            // 地址标签（每行第一个，左置对齐）
            if (col == 0) {
                val addrLayout = textMeasurer.measure(
                    cell.address,
                    TextStyle(color = TerminalGray.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                )
                drawText(addrLayout, topLeft = Offset(0f, y + (cellHeight - gap - addrLayout.size.height) / 2))
            }

            // 光标读取头（黄色外边框 + 顶部小三角）
            if (cursorAlpha > 0.01f) {
                val strokeWidth = 2.dp.toPx()
                drawRect(
                    color = TerminalAmber.copy(alpha = cursorAlpha * 0.8f),
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

        // 光标拖尾残影（2-3 个渐淡历史位置）
        if (prevCursorIndex >= 0 && prevCursorIndex != cursorIndex) {
            val prow = prevCursorIndex / columns
            val pcol = prevCursorIndex % columns
            val px = addrColWidth + pcol * cellWidth + gap / 2
            val py = prow * cellHeight + gap / 2 + 18.dp.toPx()
            val tailAlpha = (1f - cursorAnim.value) * 0.3f
            drawRect(
                color = TerminalAmber.copy(alpha = tailAlpha),
                topLeft = Offset(px - 1.dp.toPx(), py - 1.dp.toPx()),
                size = Size(cellWidth - gap + 2.dp.toPx(), cellHeight - gap + 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 静态指针虚线箭头
        pointerLinks.forEach { (sourceIndex, targetIndex) ->
            if (sourceIndex !in cells.indices || targetIndex !in cells.indices) return@forEach
            drawPointerArrow(sourceIndex, targetIndex, columns, addrColWidth, cellWidth, cellHeight, gap, TerminalGreen.copy(alpha = 0.5f))
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
                    sIdx, tIdx, columns, addrColWidth, cellWidth, cellHeight, gap,
                    TerminalGreen, pointerAnimation.progress
                )
            }
        }
    }
}

private fun DrawScope.drawBaseGrid(
    rows: Int, columns: Int,
    addrColWidth: Float, cellWidth: Float, cellHeight: Float, gap: Float
) {
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val x = addrColWidth + c * cellWidth + gap / 2
            val y = r * cellHeight + gap / 2 + 18.dp.toPx()
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(x, y),
                size = Size(cellWidth - gap, cellHeight - gap),
                style = Stroke(width = 0.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f)))
            )
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
    columns: Int, addrColWidth: Float, cellWidth: Float, cellHeight: Float, gap: Float,
    color: Color
) {
    val sourceRow = sourceIndex / columns
    val sourceCol = sourceIndex % columns
    val targetRow = targetIndex / columns
    val targetCol = targetIndex % columns

    val sourceX = addrColWidth + sourceCol * cellWidth + cellWidth / 2
    val sourceY = sourceRow * cellHeight + 20.dp.toPx() + (cellHeight - gap) / 2
    val targetX = addrColWidth + targetCol * cellWidth + cellWidth / 2
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
    columns: Int, addrColWidth: Float, cellWidth: Float, cellHeight: Float, gap: Float,
    color: Color, progress: Float
) {
    val sourceRow = sourceIndex / columns
    val sourceCol = sourceIndex % columns
    val targetRow = targetIndex / columns
    val targetCol = targetIndex % columns

    val sourceX = addrColWidth + sourceCol * cellWidth + cellWidth / 2
    val sourceY = sourceRow * cellHeight + 20.dp.toPx() + (cellHeight - gap) / 2
    val targetX = addrColWidth + targetCol * cellWidth + cellWidth / 2
    val targetY = targetRow * cellHeight + 20.dp.toPx() + (cellHeight - gap) / 2

    val start = Offset(sourceX, sourceY)
    val end = Offset(
        sourceX + (targetX - sourceX) * progress,
        sourceY + (targetY - sourceY) * progress
    )

    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 2.dp.toPx()
    )

    if (progress > 0.3f) {
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 3.dp.toPx(),
            center = end
        )
    }

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
                address = "0x%04X".format(0x1000 + i * 8),
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
