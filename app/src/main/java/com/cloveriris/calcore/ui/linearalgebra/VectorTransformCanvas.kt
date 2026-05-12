package com.cloveriris.calcore.ui.linearalgebra

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGridLine
import com.cloveriris.calcore.ui.theme.TerminalGray
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val VectorRed = Color(0xFFFF5252)
private val VectorGreen = Color(0xFF69F0AE)
private const val DEFAULT_SCALE_PX = 80f
private const val WORLD_RANGE = 5

/**
 * 2D 向量变换动画组件
 *
 * 展示矩阵变换如何改变标准基向量 e₁=(1,0) 和 e₂=(0,1)。
 * 支持从原基向量到变换后基向量的平滑插值动画。
 * 支持拖拽平移、双指缩放、双击重置。
 */
@Composable
fun VectorTransformCanvas(
    matrix: Matrix<Double>,
    modifier: Modifier = Modifier
) {
    require(matrix.rows == 2 && matrix.cols == 2) { "Matrix must be 2x2 for vector transform canvas" }

    val textMeasurer = rememberTextMeasurer()
    val animProgress = remember { Animatable(0f) }

    // Pan/zoom state
    var scalePx by remember { mutableFloatStateOf(DEFAULT_SCALE_PX) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(matrix) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(durationMillis = 400))
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scalePx = (scalePx * zoom).coerceIn(20f, 300f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scalePx = DEFAULT_SCALE_PX
                    offsetX = 0f
                    offsetY = 0f
                })
            }
    ) {
        val centerX = size.width / 2f + offsetX
        val centerY = size.height / 2f + offsetY

        drawRect(color = TerminalBackground)
        drawGrid(centerX, centerY, scalePx)
        drawAxes(centerX, centerY)

        val progress = animProgress.value

        val a11 = matrix[0, 0]
        val a21 = matrix[1, 0]
        val a12 = matrix[0, 1]
        val a22 = matrix[1, 1]

        val ae1x = (a11 * progress).toFloat()
        val ae1y = (a21 * progress).toFloat()
        val ae2x = (a12 * progress).toFloat()
        val ae2y = (a22 * progress).toFloat()

        if (progress < 1f) {
            drawUnitSquare(centerX, centerY, scalePx, 1f - progress)
        }

        if (progress > 0f) {
            drawParallelogram(centerX, centerY, ae1x, ae1y, ae2x, ae2y, scalePx, progress)
        }

        if (progress < 1f) {
            val alpha = 1f - progress
            drawArrow(
                startX = centerX, startY = centerY,
                endX = centerX + scalePx, endY = centerY,
                color = VectorRed, strokeWidth = 2f, alpha = alpha
            )
            drawArrow(
                startX = centerX, startY = centerY,
                endX = centerX, endY = centerY - scalePx,
                color = VectorGreen, strokeWidth = 2f, alpha = alpha
            )

            val labelStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TerminalGray.copy(alpha = alpha)
            )
            val e1Layout = textMeasurer.measure("e₁", labelStyle)
            val e2Layout = textMeasurer.measure("e₂", labelStyle)
            drawText(
                textLayoutResult = e1Layout,
                topLeft = Offset(centerX + scalePx + 6f, centerY - e1Layout.size.height / 2f)
            )
            drawText(
                textLayoutResult = e2Layout,
                topLeft = Offset(centerX - e2Layout.size.width / 2f, centerY - scalePx - e2Layout.size.height - 4f)
            )
        }

        if (progress > 0f) {
            val end1X = centerX + ae1x * scalePx
            val end1Y = centerY - ae1y * scalePx
            val end2X = centerX + ae2x * scalePx
            val end2Y = centerY - ae2y * scalePx

            drawArrow(
                startX = centerX, startY = centerY,
                endX = end1X, endY = end1Y,
                color = VectorRed, strokeWidth = 3f, alpha = progress
            )
            drawArrow(
                startX = centerX, startY = centerY,
                endX = end2X, endY = end2Y,
                color = VectorGreen, strokeWidth = 3f, alpha = progress
            )

            val labelStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TerminalGray.copy(alpha = progress)
            )
            val ae1Layout = textMeasurer.measure("Ae₁", labelStyle)
            val ae2Layout = textMeasurer.measure("Ae₂", labelStyle)
            drawText(
                textLayoutResult = ae1Layout,
                topLeft = Offset(end1X + 6f, end1Y - ae1Layout.size.height / 2f)
            )
            drawText(
                textLayoutResult = ae2Layout,
                topLeft = Offset(end2X - ae2Layout.size.width / 2f, end2Y - ae2Layout.size.height - 4f)
            )
        }
    }
}

private fun DrawScope.drawGrid(centerX: Float, centerY: Float, scalePx: Float) {
    for (i in -WORLD_RANGE..WORLD_RANGE) {
        val x = centerX + i * scalePx
        drawLine(
            color = TerminalGridLine,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5f
        )
    }
    for (i in -WORLD_RANGE..WORLD_RANGE) {
        val y = centerY - i * scalePx
        drawLine(
            color = TerminalGridLine,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5f
        )
    }
}

private fun DrawScope.drawAxes(centerX: Float, centerY: Float) {
    drawLine(
        color = Color.White.copy(alpha = 0.8f),
        start = Offset(0f, centerY),
        end = Offset(size.width, centerY),
        strokeWidth = 1f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.8f),
        start = Offset(centerX, 0f),
        end = Offset(centerX, size.height),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawArrow(
    startX: Float, startY: Float,
    endX: Float, endY: Float,
    color: Color, strokeWidth: Float, alpha: Float
) {
    val c = color.copy(alpha = alpha)
    drawLine(
        color = c,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth
    )

    val dx = endX - startX
    val dy = endY - startY
    val angle = atan2(dy, dx)
    val arrowLen = 10f
    val arrowAngle = 0.5f

    val x1 = endX - arrowLen * cos(angle - arrowAngle)
    val y1 = endY - arrowLen * sin(angle - arrowAngle)
    val x2 = endX - arrowLen * cos(angle + arrowAngle)
    val y2 = endY - arrowLen * sin(angle + arrowAngle)

    drawLine(color = c, start = Offset(x1, y1), end = Offset(endX, endY), strokeWidth = strokeWidth)
    drawLine(color = c, start = Offset(x2, y2), end = Offset(endX, endY), strokeWidth = strokeWidth)
}

private fun DrawScope.drawUnitSquare(centerX: Float, centerY: Float, scalePx: Float, alpha: Float) {
    val p0 = Offset(centerX, centerY)
    val p1 = Offset(centerX + scalePx, centerY)
    val p2 = Offset(centerX + scalePx, centerY - scalePx)
    val p3 = Offset(centerX, centerY - scalePx)
    val color = Color.LightGray.copy(alpha = alpha * 0.5f)

    drawLine(color, p0, p1, strokeWidth = 1f)
    drawLine(color, p1, p2, strokeWidth = 1f)
    drawLine(color, p2, p3, strokeWidth = 1f)
    drawLine(color, p3, p0, strokeWidth = 1f)
}

private fun DrawScope.drawParallelogram(
    centerX: Float, centerY: Float,
    v1x: Float, v1y: Float, v2x: Float, v2y: Float,
    scalePx: Float, alpha: Float
) {
    val p0 = Offset(centerX, centerY)
    val p1 = Offset(centerX + v1x * scalePx, centerY - v1y * scalePx)
    val p2 = Offset(centerX + (v1x + v2x) * scalePx, centerY - (v1y + v2y) * scalePx)
    val p3 = Offset(centerX + v2x * scalePx, centerY - v2y * scalePx)

    val path = Path().apply {
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }

    drawPath(path, color = Color.White.copy(alpha = alpha * 0.1f))
    drawPath(path, color = Color.White.copy(alpha = alpha * 0.5f), style = Stroke(width = 1.5f))
}
