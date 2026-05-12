package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import com.cloveriris.calcore.presentation.visualization.AddressBusVisual
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.theme.TerminalGridLine

/**
 * 地址总线可视化（L6: Address Bus）
 *
 * @param addressBus 地址总线状态，null 时不绘制内容
 */
@Composable
fun AddressBusView(
    addressBus: AddressBusVisual?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .background(TerminalBackground)
            .padding(8.dp)
    ) {
        Text(
            text = "L6: ADDRESS BUS",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (addressBus == null) return@Canvas

            val boxWidth = 86.dp.toPx()
            val boxHeight = 38.dp.toPx()
            val midGap = 32.dp.toPx()
            val totalWidth = boxWidth * 3 + midGap * 2
            val startX = (size.width - totalWidth) / 2
            val centerY = size.height / 2

            val segHex = addressBus.segment.toString(16).uppercase().padStart(4, '0')
            val offHex = addressBus.offset.toString(16).uppercase().padStart(4, '0')
            val fullHex = addressBus.fullAddress.toString(16).uppercase().padStart(8, '0')

            val progress = addressBus.progress
            val isHighlighted = progress > 0.5f

            // Segment 方块（左上）
            val segX = startX
            val segY = centerY - boxHeight - 6.dp.toPx()
            drawAddressBox(segX, segY, boxWidth, boxHeight, "SEG", segHex, isHighlighted, textMeasurer)

            // Offset 方块（左下）
            val offX = startX
            val offY = centerY + 6.dp.toPx()
            drawAddressBox(offX, offY, boxWidth, boxHeight, "OFF", offHex, isHighlighted, textMeasurer)

            // ALU 方块（中）
            val aluX = startX + boxWidth + midGap
            val aluY = centerY - boxHeight / 2
            val aluBg = if (isHighlighted) TerminalGreen.copy(alpha = 0.25f) else Color(0xFF1F1F1F)
            drawRect(
                color = aluBg,
                topLeft = Offset(aluX, aluY),
                size = Size(boxWidth, boxHeight)
            )
            drawRect(
                color = if (isHighlighted) TerminalGreen else TerminalGray,
                topLeft = Offset(aluX, aluY),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = 2.dp.toPx())
            )
            val aluTextLayout = textMeasurer.measure(
                "ALU",
                TextStyle(
                    color = if (isHighlighted) TerminalGreen else TerminalGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                aluTextLayout,
                topLeft = Offset(
                    aluX + (boxWidth - aluTextLayout.size.width) / 2,
                    aluY + (boxHeight - aluTextLayout.size.height) / 2
                )
            )

            // Full Address 方块（右）
            val fullX = aluX + boxWidth + midGap
            val fullY = centerY - boxHeight / 2
            val fullBg = if (isHighlighted) TerminalGreen.copy(alpha = 0.15f) else Color(0xFF1F1F1F)
            drawAddressBox(fullX, fullY, boxWidth, boxHeight, "ADDR", fullHex, isHighlighted, textMeasurer, overrideBg = fullBg)

            // 流光：Segment → ALU
            val segCenterRight = Offset(segX + boxWidth, segY + boxHeight / 2)
            val aluLeftTop = Offset(aluX, aluY + boxHeight * 0.35f)
            drawBusFlowCurve(segCenterRight, aluLeftTop, progress)

            // 流光：Offset → ALU
            val offCenterRight = Offset(offX + boxWidth, offY + boxHeight / 2)
            val aluLeftBot = Offset(aluX, aluY + boxHeight * 0.65f)
            drawBusFlowCurve(offCenterRight, aluLeftBot, progress)

            // 流光：ALU → Full Address（progress 0.5~1.0）
            val aluCenterRight = Offset(aluX + boxWidth, aluY + boxHeight / 2)
            val fullLeftCenter = Offset(fullX, fullY + boxHeight / 2)
            val rightProgress = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
            drawBusFlowCurve(aluCenterRight, fullLeftCenter, rightProgress)
        }
    }
}

private fun DrawScope.drawAddressBox(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    label: String,
    value: String,
    highlighted: Boolean,
    textMeasurer: TextMeasurer,
    overrideBg: Color? = null
) {
    val bg = overrideBg ?: Color(0xFF1F1F1F)
    drawRect(
        color = bg,
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    drawRect(
        color = if (highlighted) TerminalGreen.copy(alpha = 0.45f) else TerminalGridLine,
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = Stroke(width = 1.dp.toPx())
    )
    val labelLayout = textMeasurer.measure(
        label,
        TextStyle(
            color = TerminalGray,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(
        labelLayout,
        topLeft = Offset(x + 4.dp.toPx(), y + 2.dp.toPx())
    )
    val valueLayout = textMeasurer.measure(
        value,
        TextStyle(
            color = TerminalGreen,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(
        valueLayout,
        topLeft = Offset(
            x + (width - valueLayout.size.width) / 2,
            y + (height - valueLayout.size.height) / 2 + 5.dp.toPx()
        )
    )
}

private fun DrawScope.drawBusFlowCurve(
    start: Offset,
    end: Offset,
    progress: Float
) {
    if (progress <= 0f) return

    // 基线
    val path = Path().apply {
        val controlX = (start.x + end.x) / 2
        val controlY = start.y + (end.y - start.y) * 0.2f
        moveTo(start.x, start.y)
        quadraticTo(controlX, controlY, end.x, end.y)
    }
    drawPath(
        path = path,
        color = TerminalGreen.copy(alpha = 0.12f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 沿贝塞尔曲线的流光点
    val controlX = (start.x + end.x) / 2
    val controlY = start.y + (end.y - start.y) * 0.2f

    val steps = 7
    for (i in 0..steps) {
        val t = progress - (i / steps.toFloat()) * 0.18f
        if (t < 0f || t > 1f) continue
        val pos = quadBezier(t, start, Offset(controlX, controlY), end)
        val alpha = (1f - i / steps.toFloat()).coerceIn(0.1f, 1f)
        val radius = (3f - i * 0.25f).dp.toPx()
        drawCircle(
            color = TerminalGreen.copy(alpha = alpha),
            radius = radius,
            center = pos
        )
    }
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
private fun AddressBusViewPreview() {
    CalcoreTheme {
        AddressBusView(
            addressBus = AddressBusVisual(
                segment = 0x07C0L,
                offset = 0x0010L,
                fullAddress = 0x07C10L,
                progress = 0.75f
            )
        )
    }
}
