package com.cloveriris.calcore.ui.visualization

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
 * 垂直堆叠的方块，栈顶在上方，高亮显示。
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 栈顶在上方：最近 push 的在顶部
                stack.asReversed().forEachIndexed { index, op ->
                    val isTop = index == 0
                    Box(
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .border(
                                width = if (isTop) 2.dp else 1.dp,
                                color = if (isTop) TerminalAmber else TerminalGreen,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(TerminalGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = op,
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
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
