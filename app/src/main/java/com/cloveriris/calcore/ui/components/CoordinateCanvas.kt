package com.cloveriris.calcore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.graphing.GraphPoint
import com.cloveriris.calcore.domain.model.graphing.GraphShape
import com.cloveriris.calcore.domain.model.graphing.ViewportState
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGridLine
import com.cloveriris.calcore.ui.theme.TerminalGray
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun CoordinateCanvas(
    viewport: ViewportState,
    shapes: List<GraphShape>,
    onViewportChange: (ViewportState) -> Unit,
    onSizeChanged: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (viewport.scale * zoom)
                        .coerceIn(ViewportState.MIN_SCALE, ViewportState.MAX_SCALE)
                    // 先平移
                    val panCenterX = viewport.centerX - pan.x / viewport.scale
                    val panCenterY = viewport.centerY + pan.y / viewport.scale
                    // 以双指中心为锚点缩放：保持该点世界坐标不变
                    val (centroidWorldX, centroidWorldY) = viewport.screenToWorld(
                        centroid.x, centroid.y, size.width.toFloat(), size.height.toFloat()
                    )
                    val newCenterX = centroidWorldX - (centroid.x - size.width / 2f) / newScale
                    val newCenterY = centroidWorldY + (centroid.y - size.height / 2f) / newScale

                    onViewportChange(
                        viewport.copy(
                            centerX = newCenterX,
                            centerY = newCenterY,
                            scale = newScale
                        )
                    )
                }
            }
    ) {
        drawRect(color = TerminalBackground)

        val width = size.width
        val height = size.height
        onSizeChanged(width, height)

        // Grid + labels
        drawGrid(viewport, width, height, textMeasurer)

        // Axes
        val (originX, originY) = viewport.worldToScreen(0.0, 0.0, width, height)
        if (originY in 0f..height) {
            drawLine(
                color = TerminalGray,
                start = Offset(0f, originY),
                end = Offset(width, originY),
                strokeWidth = 1.5f
            )
        }
        if (originX in 0f..width) {
            drawLine(
                color = TerminalGray,
                start = Offset(originX, 0f),
                end = Offset(originX, height),
                strokeWidth = 1.5f
            )
        }

        // Shapes
        shapes.forEach { shape ->
            when (shape) {
                is GraphShape.Polyline -> drawPolyline(shape, viewport, width, height)
                is GraphShape.Points -> drawPoints(shape, viewport, width, height)
            }
        }
    }
}

private fun DrawScope.drawGrid(
    viewport: ViewportState,
    width: Float,
    height: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val rawSpacing = 50.0 / viewport.scale
    val log = log10(rawSpacing)
    val frac = log - floor(log)
    val baseSpacing = when {
        frac < 0.15 -> 1.0 * 10.0.pow(floor(log))
        frac < 0.5 -> 2.0 * 10.0.pow(floor(log))
        else -> 5.0 * 10.0.pow(floor(log))
    }

    val startX = floor((viewport.centerX - width / 2f / viewport.scale) / baseSpacing) * baseSpacing
    val endX = ceil((viewport.centerX + width / 2f / viewport.scale) / baseSpacing) * baseSpacing
    val startY = floor((viewport.centerY - height / 2f / viewport.scale) / baseSpacing) * baseSpacing
    val endY = ceil((viewport.centerY + height / 2f / viewport.scale) / baseSpacing) * baseSpacing

    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = TerminalGray
    )

    var x = startX
    while (x <= endX) {
        val (sx, _) = viewport.worldToScreen(x, 0.0, width, height)
        drawLine(
            color = TerminalGridLine,
            start = Offset(sx, 0f),
            end = Offset(sx, height),
            strokeWidth = 0.5f
        )
        // 刻度标签（仅在 X 轴附近或底部显示）
        val (_, originY) = viewport.worldToScreen(0.0, 0.0, width, height)
        val labelY = if (originY in 8f..height - 8f) originY + 14f else height - 6f
        if (sx in 8f..width - 8f && kotlin.math.abs(x) > baseSpacing * 0.001) {
            val text = formatGridLabel(x)
            val textLayout = textMeasurer.measure(text, textStyle)
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(sx - textLayout.size.width / 2f, labelY)
            )
        }
        x += baseSpacing
    }

    var y = startY
    while (y <= endY) {
        val (_, sy) = viewport.worldToScreen(0.0, y, width, height)
        drawLine(
            color = TerminalGridLine,
            start = Offset(0f, sy),
            end = Offset(width, sy),
            strokeWidth = 0.5f
        )
        // 刻度标签（仅在 Y 轴附近或左侧显示）
        val (originX, _) = viewport.worldToScreen(0.0, 0.0, width, height)
        val labelX = if (originX in 20f..width - 20f) originX - 4f else 4f
        if (sy in 10f..height - 10f && kotlin.math.abs(y) > baseSpacing * 0.001) {
            val text = formatGridLabel(y)
            val textLayout = textMeasurer.measure(text, textStyle)
            val drawX = if (labelX == 4f) labelX else labelX - textLayout.size.width
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(drawX, sy - textLayout.size.height / 2f)
            )
        }
        y += baseSpacing
    }
}

private fun formatGridLabel(value: Double): String {
    return when {
        kotlin.math.abs(value) < 1e-9 -> "0"
        kotlin.math.abs(value) >= 10000 || (kotlin.math.abs(value) < 0.001 && kotlin.math.abs(value) > 1e-9) ->
            String.format("%.1e", value)
        else -> {
            val s = String.format("%.6f", value)
                .trimEnd('0')
                .trimEnd('.')
            if (s == "-0") "0" else s
        }
    }
}

private fun DrawScope.drawPolyline(
    shape: GraphShape.Polyline,
    viewport: ViewportState,
    width: Float,
    height: Float
) {
    val points = shape.points.map {
        val (sx, sy) = viewport.worldToScreen(it.x, it.y, width, height)
        Offset(sx, sy)
    }
    if (points.size < 2) return

    val maxJump = kotlin.math.max(width, height) * 1.5f
    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        // 跳过跨屏跳跃（如 1/x 的断点两侧）
        if ((a - b).getDistance() > maxJump) continue
        drawLine(
            color = shape.color,
            start = a,
            end = b,
            strokeWidth = shape.strokeWidth
        )
    }
}

private fun DrawScope.drawPoints(
    shape: GraphShape.Points,
    viewport: ViewportState,
    width: Float,
    height: Float
) {
    shape.points.forEach {
        val (sx, sy) = viewport.worldToScreen(it.x, it.y, width, height)
        if (sx in -shape.radius..width + shape.radius && sy in -shape.radius..height + shape.radius) {
            drawCircle(
                color = shape.color,
                center = Offset(sx, sy),
                radius = shape.radius
            )
        }
    }
}
