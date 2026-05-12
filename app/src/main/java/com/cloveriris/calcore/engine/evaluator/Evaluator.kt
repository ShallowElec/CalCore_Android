package com.cloveriris.calcore.engine.evaluator

import com.cloveriris.calcore.engine.parser.BinaryOperator
import com.cloveriris.calcore.engine.parser.Expression
import com.cloveriris.calcore.engine.parser.UnaryOperator
import kotlin.math.*

/**
 * 表达式求值器
 *
 * 遍历 AST 并计算数值结果。
 */
object Evaluator {

    fun evaluate(expression: Expression, variables: Map<String, Double> = emptyMap()): Double {
        return when (expression) {
            is Expression.NumberLiteral -> expression.value
            is Expression.ConstantRef -> expression.value
            is Expression.VariableRef -> variables[expression.name] ?: 0.0
            is Expression.Binary -> evaluateBinary(expression, variables)
            is Expression.Unary -> evaluateUnary(expression, variables)
            is Expression.FunctionCall -> evaluateFunction(expression, variables)
        }
    }

    private fun evaluateBinary(expr: Expression.Binary, variables: Map<String, Double>): Double {
        val left = evaluate(expr.left, variables)
        val right = evaluate(expr.right, variables)

        return when (expr.operator) {
            BinaryOperator.ADD -> left + right
            BinaryOperator.SUBTRACT -> left - right
            BinaryOperator.MULTIPLY -> left * right
            BinaryOperator.DIVIDE -> if (right != 0.0) left / right else Double.NaN
            BinaryOperator.POWER -> left.pow(right)
        }
    }

    private fun evaluateUnary(expr: Expression.Unary, variables: Map<String, Double>): Double {
        val operand = evaluate(expr.operand, variables)
        return when (expr.operator) {
            UnaryOperator.NEGATE -> -operand
            UnaryOperator.PERCENT -> operand / 100.0
            UnaryOperator.FACTORIAL -> factorial(operand)
        }
    }

    private fun factorial(n: Double): Double {
        if (n < 0) return Double.NaN
        if (n > 170) return Double.POSITIVE_INFINITY // 170! 是 Double 能表示的最大值
        val intN = n.toInt()
        if (intN.toDouble() != n) return Double.NaN // 非整数阶乘暂不支持
        var result = 1.0
        for (i in 2..intN) {
            result *= i
        }
        return result
    }

    private fun evaluateFunction(expr: Expression.FunctionCall, variables: Map<String, Double>): Double {
        val arg = evaluate(expr.argument, variables)
        return when (expr.name.lowercase()) {
            "sin" -> sin(arg)
            "cos" -> cos(arg)
            "tan" -> tan(arg)
            "asin" -> asin(arg)
            "acos" -> acos(arg)
            "atan" -> atan(arg)
            "sinh" -> sinh(arg)
            "cosh" -> cosh(arg)
            "tanh" -> tanh(arg)
            "log" -> log10(arg)
            "ln" -> ln(arg)
            "sqrt" -> sqrt(arg)
            "cbrt" -> cbrt(arg)
            "abs" -> abs(arg)
            "floor" -> floor(arg)
            "ceil" -> ceil(arg)
            "round" -> round(arg)
            else -> Double.NaN
        }
    }
}
