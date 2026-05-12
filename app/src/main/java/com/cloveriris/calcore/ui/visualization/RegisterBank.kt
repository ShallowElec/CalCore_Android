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
import com.cloveriris.calcore.presentation.visualization.DataPathVisual
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalAmber
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.theme.TerminalGray

/**
 * 寄存器组可视化
 *
 * @param registers 寄存器列表，每个包含名称和 64-bit 值
 * @param dataPath 数据搬运路径，非 null 时绘制绿色流光箭头
 */
data class RegisterVisual(
    val name: String,
    val value: Long,
    val isHighlighted: Boolean = false
)

@Composable
fun RegisterBank(
    registers: List<RegisterVisual>,
    modifier: Modifier = Modifier,
    dataPath: DataPathVisual? = null
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((registers.size * 40 + 16).dp)
            .padding(8.dp)
    ) {
        val rowHeight = 36.dp.toPx()
        val nameWidth = 60.dp.toPx()
        val gap = 4.dp.toPx()

        registers.forEachIndexed { index, reg ->
            val y = index * rowHeight + gap

            // 寄存器名背景
            drawRect(
                color = if (reg.isHighlighted) TerminalAmber.copy(alpha = 0.3f) else Color(0xFF1F1F1F),
                topLeft = Offset(0f, y),
                size = Size(nameWidth, rowHeight - gap)
            )

            // 寄存器名文字
            val nameLayout = textMeasurer.measure(
                reg.name,
                TextStyle(
                    color = if (reg.isHighlighted) TerminalAmber else TerminalGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                nameLayout,
                topLeft = Offset(
                    (nameWidth - nameLayout.size.width) / 2,
                    y + (rowHeight - gap - nameLayout.size.height) / 2
                )
            )

            // 64-bit 值可视化（16 个 4-bit 块）
            val hexDigits = reg.value.toString(16).uppercase().padStart(16, '0')
            val blockWidth = (size.width - nameWidth - 8.dp.toPx()) / 16f

            for (i in 0 until 16) {
                val blockX = nameWidth + 8.dp.toPx() + i * blockWidth
                val nibble = hexDigits[i].digitToIntOrNull(16) ?: 0
                val intensity = nibble / 15f

                drawRect(
                    color = TerminalGreen.copy(alpha = 0.1f + intensity * 0.9f),
                    topLeft = Offset(blockX, y),
                    size = Size(blockWidth - gap, rowHeight - gap)
                )

                // Hex 数字
                val hexLayout = textMeasurer.measure(
                    hexDigits[i].toString(),
                    TextStyle(
                        color = if (intensity > 0.5f) Color.Black else TerminalGreen,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                drawText(
                    hexLayout,
                    topLeft = Offset(
                        blockX + (blockWidth - gap - hexLayout.size.width) / 2,
                        y + (rowHeight - gap - hexLayout.size.height) / 2
                    )
                )
            }
        }

        // 数据路径绿色流光箭头
        if (dataPath != null) {
            val fromIndex = registers.indexOfFirst { it.name == dataPath.from }
            val toIndex = registers.indexOfFirst { it.name == dataPath.to }
            if (fromIndex >= 0 && toIndex >= 0) {
                val fromY = fromIndex * rowHeight + gap + (rowHeight - gap) / 2
                val toY = toIndex * rowHeight + gap + (rowHeight - gap) / 2
                val pathX = nameWidth + 24.dp.toPx()
                val start = Offset(pathX, fromY)
                val end = Offset(pathX, toY)
                val control = Offset(pathX + 36.dp.toPx(), (fromY + toY) / 2)

                // 基线
                val basePath = Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(control.x, control.y, end.x, end.y)
                }
                drawPath(
                    path = basePath,
                    color = TerminalGreen.copy(alpha = 0.2f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 流光小圆点
                val steps = 6
                for (i in 0..steps) {
                    val t = dataPath.progress - (i / steps.toFloat()) * 0.12f
                    if (t < 0f || t > 1f) continue
                    val pos = quadBezier(t, start, control, end)
                    val alpha = (1f - i / steps.toFloat()).coerceIn(0.15f, 1f)
                    val radius = (3.5f - i * 0.3f).dp.toPx()
                    drawCircle(
                        color = TerminalGreen.copy(alpha = alpha),
                        radius = radius,
                        center = pos
                    )
                }

                // 箭头头部
                if (dataPath.progress > 0.85f) {
                    drawArrowHead(end, control, end, TerminalGreen)
                }
            }
        }
    }
}

private fun DrawScope.drawArrowHead(tip: Offset, control: Offset, end: Offset, color: Color) {
    // 计算终点切线方向
    val dx = end.x - control.x
    val dy = end.y - control.y
    val len = kotlin.math.hypot(dx, dy)
    if (len < 0.001f) return
    val ux = dx / len
    val uy = dy / len

    val arrowLen = 8.dp.toPx()
    val arrowWidth = 5.dp.toPx()

    val baseX = tip.x - ux * arrowLen
    val baseY = tip.y - uy * arrowLen
    val perpX = -uy * arrowWidth
    val perpY = ux * arrowWidth

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(baseX + perpX, baseY + perpY)
        lineTo(baseX - perpX, baseY - perpY)
        close()
    }
    drawPath(path = path, color = color)
}

private fun quadBezier(t: Float, p0: Offset, p1: Offset, p2: Offset): Offset {
    val oneMinusT = 1f - t
    return Offset(
        oneMinusT * oneMinusT * p0.x + 2f * oneMinusT * t * p1.x + t * t * p2.x,
        oneMinusT * oneMinusT * p0.y + 2f * oneMinusT * t * p1.y + t * t * p2.y
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun RegisterBankPreview() {
    CalcoreTheme {
        val registers = listOf(
            RegisterVisual("RAX", 0x0000000000000042L, isHighlighted = true),
            RegisterVisual("RBX", 0x0000000000000000L),
            RegisterVisual("RCX", 0x00007FFE_EFBCA000L),
            RegisterVisual("RDX", 0x0000000000000001L),
            RegisterVisual("RSI", 0x0000000000000000L),
            RegisterVisual("RDI", 0x0000000000000000L),
            RegisterVisual("RSP", 0x00007FFE_EFBC9000L),
            RegisterVisual("RBP", 0x00007FFE_EFBC9010L)
        )
        RegisterBank(
            registers = registers,
            dataPath = DataPathVisual(from = "RAX", to = "RCX", progress = 0.6f)
        )
    }
}
