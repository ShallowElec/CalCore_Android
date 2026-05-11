package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.engine.parser.BinaryOperator
import com.cloveriris.calcore.engine.parser.Expression
import com.cloveriris.calcore.engine.parser.UnaryOperator
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * AST 树形可视化
 *
 * 递归渲染表达式树，节点用圆角矩形表示，父子节点用连线连接。
 */
@Composable
fun AstTreeView(
    expression: Expression?,
    modifier: Modifier = Modifier,
    highlightedNode: Expression? = null
) {
    if (expression == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expression to visualize",
                color = TerminalGray.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AstNode(
            expression = expression,
            highlightedNode = highlightedNode,
            isRoot = true
        )
    }
}

@Composable
private fun AstNode(
    expression: Expression,
    highlightedNode: Expression?,
    isRoot: Boolean = false
) {
    val isHighlighted = expression == highlightedNode
    val nodeColor = when {
        isHighlighted -> TerminalGreen.copy(alpha = 0.3f)
        else -> TerminalGray.copy(alpha = 0.15f)
    }
    val textColor = when {
        isHighlighted -> TerminalGreen
        else -> TerminalGray.copy(alpha = 0.9f)
    }
    val label = expressionLabel(expression)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 节点标签
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 12.dp)
                .background(nodeColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }

        // 子节点
        val children = expressionChildren(expression)
        if (children.isNotEmpty()) {
            // 连线
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(TerminalGray.copy(alpha = 0.3f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                children.forEach { child ->
                    AstNode(
                        expression = child,
                        highlightedNode = highlightedNode
                    )
                }
            }
        }
    }
}

private fun expressionLabel(expr: Expression): String = when (expr) {
    is Expression.NumberLiteral -> {
        val v = expr.value
        if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.3f".format(v)
    }
    is Expression.Binary -> operatorSymbol(expr.operator)
    is Expression.Unary -> operatorSymbol(expr.operator)
    is Expression.FunctionCall -> "${expr.name}()"
    is Expression.ConstantRef -> expr.name
}

private fun expressionChildren(expr: Expression): List<Expression> = when (expr) {
    is Expression.NumberLiteral -> emptyList()
    is Expression.Binary -> listOf(expr.left, expr.right)
    is Expression.Unary -> listOf(expr.operand)
    is Expression.FunctionCall -> listOf(expr.argument)
    is Expression.ConstantRef -> emptyList()
}

private fun operatorSymbol(op: BinaryOperator): String = when (op) {
    BinaryOperator.ADD -> "+"
    BinaryOperator.SUBTRACT -> "-"
    BinaryOperator.MULTIPLY -> "×"
    BinaryOperator.DIVIDE -> "÷"
    BinaryOperator.POWER -> "^"
}

private fun operatorSymbol(op: UnaryOperator): String = when (op) {
    UnaryOperator.NEGATE -> "-"
    UnaryOperator.PERCENT -> "%"
}
