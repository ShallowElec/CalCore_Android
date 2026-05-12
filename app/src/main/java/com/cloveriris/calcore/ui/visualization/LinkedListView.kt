package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.presentation.visualization.LinkedListNodeVisual
import com.cloveriris.calcore.presentation.visualization.LinkedListWireState
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalAmber
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * L5 链表可视化
 *
 * 特性：
 * - 绿色实心方块：已分配的节点数据
 * - 绿色空心方块：指针节点 / next 引用
 * - 连线生长动画：指针赋值时连线从源地址“生长”到目标地址
 * - 节点滑入动画：新节点插入时缩放淡入
 */
@Composable
fun LinkedListView(
    nodes: List<LinkedListNodeVisual>,
    modifier: Modifier = Modifier,
    wireState: LinkedListWireState = LinkedListWireState()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .padding(12.dp)
    ) {
        Text(
            text = "L5: LINKED LIST",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (nodes.isEmpty()) {
            Text(
                text = "Empty list",
                color = TerminalGray.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            // 使用 Box 包裹 Row，以便在上方叠加 Canvas 绘制连线
            Box(modifier = Modifier.fillMaxWidth()) {
                // 节点行
                LinkedListNodesRow(nodes = nodes)

                // 连线生长动画层
                if (wireState.progress > 0f && wireState.fromNodeId.isNotEmpty() && wireState.toNodeId.isNotEmpty()) {
                    LinkedListWireOverlay(
                        nodes = nodes,
                        wireState = wireState,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkedListNodesRow(nodes: List<LinkedListNodeVisual>) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        nodes.forEachIndexed { index, node ->
            LinkedListNodeItem(node = node)

            val isLast = node.nextId == null
            if (isLast) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "→",
                    color = TerminalGreen,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                NullTerminator()
            } else {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "→",
                    color = TerminalGreen,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun LinkedListNodeItem(node: LinkedListNodeVisual) {
    val animProgress = remember(node.id) {
        Animatable(if (node.isNew) 0f else 1f)
    }
    LaunchedEffect(node.isNew) {
        if (node.isNew) {
            animProgress.animateTo(1f, tween(500))
        }
    }

    val scale = 0.5f + 0.5f * animProgress.value
    val borderColor = if (node.isHighlighted) TerminalAmber else TerminalGreen
    val bgColor = if (node.isPointer) Color.Transparent else TerminalGreen

    // 高亮脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "node-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .drawBehind {
                // 高亮脉冲外圈
                if (node.isHighlighted) {
                    val pulseStroke = 2.dp.toPx()
                    val pulseInset = 2.dp.toPx() + pulseStroke
                    drawRoundRect(
                        color = TerminalAmber.copy(alpha = pulseAlpha * 0.5f),
                        topLeft = Offset(-pulseInset, -pulseInset),
                        size = androidx.compose.ui.geometry.Size(
                            size.width + pulseInset * 2,
                            size.height + pulseInset * 2
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            6.dp.toPx(), 6.dp.toPx()
                        ),
                        style = Stroke(width = pulseStroke)
                    )
                }
                // 绘制方块背景
                if (node.isPointer) {
                    // 空心方块 = 指针 / 引用
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    // 实心方块 = 数据节点
                    drawRoundRect(
                        color = TerminalGreen,
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = node.value,
            color = if (node.isPointer) TerminalGreen else Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun NullTerminator() {
    Box(
        modifier = Modifier
            .background(Color(0xFF1F1F1F), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NULL",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * 链表连线生长动画 overlay
 *
 * 在节点行上方绘制从源节点指向目标节点的绿色生长曲线。
 */
@Composable
private fun LinkedListWireOverlay(
    nodes: List<LinkedListNodeVisual>,
    wireState: LinkedListWireState,
    modifier: Modifier = Modifier
) {
    val progressAnim = remember(wireState.fromNodeId, wireState.toNodeId) {
        Animatable(0f)
    }
    LaunchedEffect(wireState.progress) {
        progressAnim.animateTo(wireState.progress, tween(300))
    }

    val fromIndex = nodes.indexOfFirst { it.id == wireState.fromNodeId }
    val toIndex = nodes.indexOfFirst { it.id == wireState.toNodeId }
    if (fromIndex < 0 || toIndex < 0) return

    Canvas(modifier = modifier) {
        val nodeSize = 48.dp.toPx()
        val arrowGap = 4.dp.toPx()
        val arrowWidth = 18.dp.toPx() // "→" 的近似宽度
        val startX = fromIndex * (nodeSize + arrowGap * 2 + arrowWidth) + nodeSize + arrowGap
        val endX = toIndex * (nodeSize + arrowGap * 2 + arrowWidth)
        val y = size.height / 2f

        // 生长中的连线（贝塞尔曲线）
        val path = Path().apply {
            moveTo(startX, y)
            val controlX = (startX + endX) / 2
            val controlY = y - 20.dp.toPx() * (1f - kotlin.math.abs(toIndex - fromIndex) * 0.1f)
            quadraticTo(controlX, controlY, endX, y)
        }

        // 基线（淡绿色）
        drawPath(
            path = path,
            color = TerminalGreen.copy(alpha = 0.15f),
            style = Stroke(width = 2.dp.toPx())
        )

        // 流光生长点
        val steps = 6
        for (i in 0..steps) {
            val t = progressAnim.value - (i / steps.toFloat()) * 0.15f
            if (t < 0f || t > 1f) continue
            val pos = quadBezier(
                t,
                Offset(startX, y),
                Offset((startX + endX) / 2, y - 20.dp.toPx()),
                Offset(endX, y)
            )
            val alpha = (1f - i / steps.toFloat()).coerceIn(0.15f, 1f)
            val radius = (3.5f - i * 0.3f).dp.toPx()
            drawCircle(
                color = TerminalGreen.copy(alpha = alpha),
                radius = radius,
                center = pos
            )
        }

        // 箭头头部
        if (progressAnim.value > 0.85f) {
            drawArrowHead(
                tip = Offset(endX, y),
                direction = Offset(-1f, 0f),
                color = TerminalGreen
            )
        }
    }
}

private fun DrawScope.drawArrowHead(tip: Offset, direction: Offset, color: Color) {
    val len = kotlin.math.hypot(direction.x, direction.y)
    if (len < 0.001f) return
    val ux = direction.x / len
    val uy = direction.y / len
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
private fun LinkedListViewPreview() {
    CalcoreTheme {
        Column {
            LinkedListView(
                nodes = listOf(
                    LinkedListNodeVisual("a", "10", "b", isHighlighted = true),
                    LinkedListNodeVisual("b", "20", "c", isNew = true),
                    LinkedListNodeVisual("c", "30", null)
                ),
                wireState = LinkedListWireState("b", "c", progress = 0.75f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinkedListView(
                nodes = listOf(
                    LinkedListNodeVisual("head", "ptr", "a", isPointer = true),
                    LinkedListNodeVisual("a", "10", null)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinkedListView(nodes = emptyList())
        }
    }
}
