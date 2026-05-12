package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.presentation.visualization.DisplayBufferVisual
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.LocalVisualizationColors

/**
 * L8 显示缓冲区可视化
 *
 * 模拟 LCD 屏幕，带闪烁光标和数据流入指示。
 */
@Composable
fun DisplayBufferView(
    buffer: DisplayBufferVisual?,
    modifier: Modifier = Modifier
) {
    val viz = LocalVisualizationColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "bufferAnim")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(viz.stageBg)
            .padding(12.dp)
    ) {
        Text(
            text = "L8: DISPLAY BUFFER",
            color = viz.textMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 数据流入指示器
            DataInflowIndicator()

            // LCD 屏幕区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, viz.textMuted, RoundedCornerShape(8.dp))
                    .background(viz.stageBg, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                // LCD 局部扫描线 overlay
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    val lineCount = 6
                    val spacing = size.height / lineCount
                    for (i in 0..lineCount) {
                        drawLine(
                            color = viz.dataPrimary.copy(alpha = 0.025f),
                            start = Offset(0f, i * spacing),
                            end = Offset(size.width, i * spacing),
                            strokeWidth = 0.5f.dp.toPx()
                        )
                    }
                }

                val text = buffer?.text ?: ""
                val cursorPos = (buffer?.cursorPosition ?: 0).coerceIn(0, text.length)
                val isTyping = buffer?.isTyping == true

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 文本渲染（支持 isTyping 最后一个字符淡入）
                    if (text.isNotEmpty()) {
                        val lastCharAlpha by animateFloatAsState(
                            targetValue = if (isTyping) 1f else 1f,
                            animationSpec = tween(300),
                            label = "lastCharAlpha"
                        )

                        if (isTyping && text.length > 1) {
                            // 除最后一个字符外的文本
                            Text(
                                text = text.take(text.length - 1),
                                color = viz.dataPrimary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            // 最后一个字符带淡入
                            Text(
                                text = text.last().toString(),
                                color = viz.dataPrimary.copy(alpha = lastCharAlpha),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = text,
                                color = viz.dataPrimary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 闪烁光标（下划线）
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(18.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .alpha(cursorAlpha)
                                .background(viz.dataPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataInflowIndicator() {
    val viz = LocalVisualizationColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "inflowAnim")
    val dotAlphas = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = index * 150),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$index"
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        dotAlphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .alpha(alpha.value)
                    .background(viz.dataPrimary, CircleShape)
            )
        }
        Text(
            text = "▶",
            color = viz.dataPrimary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun DisplayBufferViewPreview() {
    CalcoreTheme {
        Column {
            DisplayBufferView(
                buffer = DisplayBufferVisual(
                    text = "3.14159",
                    cursorPosition = 7,
                    isTyping = true
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            DisplayBufferView(buffer = null)
        }
    }
}
