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

    fun evaluate(expression: Expression): Double {
        return when (expression) {
            is Expression.NumberLiteral -> expression.value
            is Expression.ConstantRef -> expression.value
            is Expression.Binary -> evaluateBinary(expression)
            is Expression.Unary -> evaluateUnary(expression)
            is Expression.FunctionCall -> evaluateFunction(expression)
        }
    }

    private fun evaluateBinary(expr: Expression.Binary): Double {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator) {
            BinaryOperator.ADD -> left + right
            BinaryOperator.SUBTRACT -> left - right
            BinaryOperator.MULTIPLY -> left * right
            BinaryOperator.DIVIDE -> if (right != 0.0) left / right else Double.NaN
            BinaryOperator.POWER -> left.pow(right)
        }
    }

    private fun evaluateUnary(expr: Expression.Unary): Double {
        val operand = evaluate(expr.operand)
        return when (expr.operator) {
            UnaryOperator.NEGATE -> -operand
            UnaryOperator.PERCENT -> operand / 100.0
        }
    }

    private fun evaluateFunction(expr: Expression.FunctionCall): Double {
        val arg = evaluate(expr.argument)
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
            "abs" -> abs(arg)
            "floor" -> floor(arg)
            "ceil" -> ceil(arg)
            "round" -> round(arg)
            else -> Double.NaN
        }
    }
}
