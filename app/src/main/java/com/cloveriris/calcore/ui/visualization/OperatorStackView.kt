package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalAmber
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * L5 运算符栈可视化
 *
 * 特性：
 * - Push：新元素从上方屏幕外滑入栈顶，带 scaleIn(0.5→1.0) + slideInVertically
 * - Pop：栈顶元素向下滑出并淡出
 * - 栈顶高亮：琥珀色边框 + 微微上下浮动
 * - 栈元素之间有细竖线连接
 */
@Composable
fun OperatorStackView(
    stack: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .padding(12.dp)
    ) {
        Text(
            text = "L5: OPERATOR STACK",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (stack.isEmpty()) {
            Text(
                text = "Empty stack",
                color = TerminalGray.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 栈顶在上方：最近 push 的在顶部
                stack.asReversed().forEachIndexed { index, op ->
                    val isTop = index == 0
                    key(op + index) {
                        StackItem(
                            label = op,
                            isTop = isTop,
                            index = index
                        )
                    }
                    // 栈元素之间的连接竖线（除最后一个）
                    if (index < stack.size - 1) {
                        Box(
                            modifier = Modifier
                                .padding(start = 24.dp)
                                .widthIn(min = 48.dp)
                                .height(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 24.dp)
                                    .height(1.dp)
                                    .background(TerminalGreen.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StackItem(
    label: String,
    isTop: Boolean,
    index: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stack-bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isTop) 2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(350)
        ) + scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(350)
        ) + fadeIn(tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + scaleOut(
            targetScale = 0.5f,
            animationSpec = tween(300)
        ) + fadeOut(tween(200))
    ) {
        val borderColor = if (isTop) TerminalAmber else TerminalGreen
        val bgColor = if (isTop) TerminalGreen.copy(alpha = 0.15f) else TerminalGreen
        val textColor = if (isTop) TerminalAmber else Color.Black

        Box(
            modifier = Modifier
                .padding(top = if (isTop) bobOffset.dp else 0.dp)
                .widthIn(min = 48.dp)
                .border(
                    width = if (isTop) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .background(bgColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun OperatorStackViewPreview() {
    CalcoreTheme {
        Column {
            OperatorStackView(stack = listOf("+", "×", "("))
            Spacer(modifier = Modifier.height(8.dp))
            OperatorStackView(stack = emptyList())
        }
    }
}
