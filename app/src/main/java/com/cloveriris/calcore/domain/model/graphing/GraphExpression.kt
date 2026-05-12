package com.cloveriris.calcore.domain.model.graphing

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * 图形模式中的表达式条目
 */
data class GraphExpression(
    val id: String = UUID.randomUUID().toString(),
    val rawExpression: String,
    val rawExpressionSecondary: String? = null,
    val displayExpression: String = rawExpression,
    val color: Color,
    val isVisible: Boolean = true,
    val type: ExpressionType = ExpressionType.EXPLICIT_Y
)

enum class ExpressionType {
    EXPLICIT_Y,      // y = f(x)
    EXPLICIT_X,      // x = f(y)
    PARAMETRIC,      // (x(t), y(t))
    POLAR,           // r = f(θ)
    IMPLICIT         // F(x,y) = 0
}
