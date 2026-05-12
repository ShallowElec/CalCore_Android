package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

    // 值变化扫描线动画：检测值发生变化的寄存器行
    val prevValues = remember(registers) { registers.map { it.value } }
    val scanAnims = remember { mutableStateMapOf<Int, Animatable<Float, *>>() }

    LaunchedEffect(registers) {
        registers.forEachIndexed { index, reg ->
            if (index < prevValues.size && reg.value != prevValues[index]) {
                val anim = scanAnims.getOrPut(index) { Animatable(0f) }
                anim.snapTo(0f)
                anim.animateTo(1f, tween(400))
                anim.animateTo(0f, tween(200))
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((registers.size * 40 + 16).dp)
            .padding(8.dp)
    ) {
        val rowHeight = 36.dp.toPx()
        val nameWidth = 64.dp.toPx()
        val gap = 4.dp.toPx()

        registers.forEachIndexed { index, reg ->
            val y = index * rowHeight + gap

            // 寄存器名背景：高亮时琥珀色脉冲，非零值时绿色细边框，零值时深灰
            val nameBgColor = when {
                reg.isHighlighted -> TerminalAmber.copy(alpha = 0.25f)
                reg.value != 0L -> TerminalGreen.copy(alpha = 0.08f)
                else -> Color(0xFF1A1A1A)
            }
            val nameTextColor = when {
                reg.isHighlighted -> TerminalAmber
                reg.value != 0L -> TerminalGreen.copy(alpha = 0.85f)
                else -> TerminalGray.copy(alpha = 0.5f)
            }
            val nameBorderColor = when {
                reg.isHighlighted -> TerminalAmber.copy(alpha = 0.6f)
                reg.value != 0L -> TerminalGreen.copy(alpha = 0.3f)
                else -> Color(0xFF2A2A2A)
            }

            drawRect(
                color = nameBgColor,
                topLeft = Offset(0f, y),
                size = Size(nameWidth, rowHeight - gap)
            )
            drawRect(
                color = nameBorderColor,
                topLeft = Offset(0f, y),
                size = Size(nameWidth, rowHeight - gap),
                style = Stroke(width = if (reg.isHighlighted) 2.dp.toPx() else 1.dp.toPx())
            )

            // 寄存器名文字
            val nameLayout = textMeasurer.measure(
                reg.name,
                TextStyle(
                    color = nameTextColor,
                    fontSize = 11.sp,
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
            val blockWidth = (size.width - nameWidth - 12.dp.toPx()) / 16f

            for (i in 0 until 16) {
                val blockX = nameWidth + 10.dp.toPx() + i * blockWidth
                val nibble = hexDigits[i].digitToIntOrNull(16) ?: 0
                val intensity = nibble / 15f

                val blockBgColor = when {
                    reg.isHighlighted -> TerminalAmber.copy(alpha = 0.1f + intensity * 0.5f)
                    reg.value != 0L -> TerminalGreen.copy(alpha = 0.08f + intensity * 0.75f)
                    else -> Color(0xFF151515)
                }
                val blockBorderColor = when {
                    reg.isHighlighted -> TerminalAmber.copy(alpha = 0.15f)
                    reg.value != 0L -> TerminalGreen.copy(alpha = 0.12f)
                    else -> Color(0xFF222222)
                }

                drawRect(
                    color = blockBgColor,
                    topLeft = Offset(blockX, y + 1.dp.toPx()),
                    size = Size(blockWidth - gap, rowHeight - gap - 2.dp.toPx())
                )
                drawRect(
                    color = blockBorderColor,
                    topLeft = Offset(blockX, y + 1.dp.toPx()),
                    size = Size(blockWidth - gap, rowHeight - gap - 2.dp.toPx()),
                    style = Stroke(width = 0.5f)
                )

                // Hex 数字
                val hexTextColor = when {
                    reg.isHighlighted -> if (intensity > 0.5f) TerminalAmber.copy(alpha = 0.9f) else TerminalAmber.copy(alpha = 0.5f)
                    reg.value != 0L -> if (intensity > 0.5f) Color.Black else TerminalGreen.copy(alpha = 0.7f)
                    else -> TerminalGray.copy(alpha = 0.25f)
                }
                val hexLayout = textMeasurer.measure(
                    hexDigits[i].toString(),
                    TextStyle(
                        color = hexTextColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                drawText(
                    hexLayout,
                    topLeft = Offset(
                        blockX + (blockWidth - gap - hexLayout.size.width) / 2,
                        y + 1.dp.toPx() + (rowHeight - gap - 2.dp.toPx() - hexLayout.size.height) / 2
                    )
                )
            }

            // 值变化扫描线（水平光束从左向右扫过）
            val scanProgress = scanAnims[index]?.value ?: 0f
            if (scanProgress > 0.01f) {
                val beamX = size.width * scanProgress
                val beamWidth = 24.dp.toPx()
                val beamAlpha = (1f - kotlin.math.abs(scanProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                drawRect(
                    color = TerminalGreen.copy(alpha = 0.25f * beamAlpha),
                    topLeft = Offset((beamX - beamWidth).coerceAtLeast(0f), y),
                    size = Size(beamWidth.coerceAtMost(beamX), rowHeight - gap)
                )
                // 中心高亮细线
                drawLine(
                    color = Color.White.copy(alpha = 0.6f * beamAlpha),
                    start = Offset(beamX, y),
                    end = Offset(beamX, y + rowHeight - gap),
                    strokeWidth = 1.5f.dp.toPx()
                )
            }
        }

        // 数据路径绿色流光箭头（增强版：多粒子拖尾 + 加粗 + 渐变）
        if (dataPath != null) {
            val fromIndex = registers.indexOfFirst { it.name == dataPath.from }
            val toIndex = registers.indexOfFirst { it.name == dataPath.to }
            if (fromIndex >= 0 && toIndex >= 0) {
                val fromY = fromIndex * rowHeight + gap + (rowHeight - gap) / 2
                val toY = toIndex * rowHeight + gap + (rowHeight - gap) / 2
                val pathX = nameWidth + 20.dp.toPx()
                val start = Offset(pathX, fromY)
                val end = Offset(pathX, toY)
                val control = Offset(pathX + 32.dp.toPx(), (fromY + toY) / 2)

                // 基线（加粗渐变）
                val basePath = Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(control.x, control.y, end.x, end.y)
                }
                drawPath(
                    path = basePath,
                    color = TerminalGreen.copy(alpha = 0.15f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawPath(
                    path = basePath,
                    color = TerminalGreen.copy(alpha = 0.08f),
                    style = Stroke(width = 6.dp.toPx())
                )

                // 多粒子拖尾数据流（8 个粒子形成流光带）
                val steps = 8
                for (i in 0..steps) {
                    val t = dataPath.progress - (i / steps.toFloat()) * 0.14f
                    if (t < 0f || t > 1f) continue
                    val pos = quadBezier(t, start, control, end)
                    val alpha = ((1f - i / steps.toFloat()) * 0.9f).coerceIn(0.1f, 1f) * dataPath.progress.coerceAtLeast(0.3f)
                    val radius = (4.5f - i * 0.35f).coerceAtLeast(1.5f).dp.toPx()
                    val color = when {
                        i == 0 -> Color.White
                        i <= 2 -> TerminalGreen.copy(alpha = 1f)
                        else -> TerminalGreen
                    }
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = radius,
                        center = pos
                    )
                }

                // 箭头头部
                if (dataPath.progress > 0.8f) {
                    drawArrowHead(end, control, end, TerminalGreen.copy(alpha = dataPath.progress))
                }

                // 路径经过的寄存器微高亮
                val midIndex = (fromIndex + toIndex) / 2
                listOf(fromIndex, midIndex, toIndex).distinct().forEach { idx ->
                    if (idx in registers.indices) {
                        val ry = idx * rowHeight + gap
                        drawRect(
                            color = TerminalGreen.copy(alpha = 0.06f * dataPath.progress),
                            topLeft = Offset(0f, ry),
                            size = Size(size.width, rowHeight - gap)
                        )
                    }
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
