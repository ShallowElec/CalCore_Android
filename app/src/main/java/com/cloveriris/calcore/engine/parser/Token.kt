package com.cloveriris.calcore.engine.parser

/**
 * 词法单元（Token）
 */
sealed class Token {
    abstract val literal: String

    // 数字字面量
    data class Number(override val literal: String, val value: Double) : Token()

    // 运算符
    data class Plus(override val literal: String = "+") : Token()
    data class Minus(override val literal: String = "-") : Token()
    data class Multiply(override val literal: String = "×") : Token()
    data class Divide(override val literal: String = "÷") : Token()
    data class Power(override val literal: String = "^") : Token()
    data class Percent(override val literal: String = "%") : Token()

    // 括号
    data class LParen(override val literal: String = "(") : Token()
    data class RParen(override val literal: String = ")") : Token()

    // 函数
    data class Function(override val literal: String) : Token()

    // 常量
    data class Constant(override val literal: String, val value: Double) : Token()

    // 变量（如 x, y, t, a, b...）
    data class Variable(override val literal: String) : Token()

    // 结束符
    data object EOF : Token() {
        override val literal: String = ""
    }

    // 非法字符
    data class Illegal(override val literal: String) : Token()
}
