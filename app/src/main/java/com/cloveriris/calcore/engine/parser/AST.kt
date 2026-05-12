package com.cloveriris.calcore.engine.parser

/**
 * 抽象语法树（AST）节点
 */
sealed class Expression {
    /**
     * 数字字面量，如 123, 3.14
     */
    data class NumberLiteral(val value: Double) : Expression()

    /**
     * 二元表达式，如 a + b, a * b
     */
    data class Binary(
        val left: Expression,
        val operator: BinaryOperator,
        val right: Expression
    ) : Expression()

    /**
     * 一元表达式，如 -a, √a
     */
    data class Unary(
        val operator: UnaryOperator,
        val operand: Expression
    ) : Expression()

    /**
     * 函数调用，如 sin(x), log(10)
     */
    data class FunctionCall(
        val name: String,
        val argument: Expression
    ) : Expression()

    /**
     * 常量引用，如 π, e
     */
    data class ConstantRef(val name: String, val value: Double) : Expression()

    /**
     * 变量引用，如 x, y, t, a, b...
     */
    data class VariableRef(val name: String) : Expression()
}

enum class BinaryOperator {
    ADD,      // +
    SUBTRACT, // -
    MULTIPLY, // ×
    DIVIDE,   // ÷
    POWER     // ^
}

enum class UnaryOperator {
    NEGATE,    // -
    PERCENT,   // %
    FACTORIAL  // !
}
