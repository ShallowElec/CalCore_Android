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
import com.cloveriris.calcore.ui.theme.TerminalAmber
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.theme.TerminalGray

/**
 * 寄存器组可视化
 *
 * @param registers 寄存器列表，每个包含名称和 64-bit 值
 */
data class RegisterVisual(
    val name: String,
    val value: Long,
    val isHighlighted: Boolean = false
)

@Composable
fun RegisterBank(
    registers: List<RegisterVisual>,
    modifier: Modifier = Modifier
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
    }
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
        RegisterBank(registers = registers)
    }
}
