package com.cloveriris.calcore.domain.model

/**
 * 计算器核心状态机
 *
 * - [Idle]: 初始状态，显示 "0"，等待输入
 * - [Inputting]: 正在输入表达式，显示当前输入
 * - [Evaluated]: 已求值，显示表达式和结果
 */
sealed interface CalculatorState {
    val displayExpression: String
    val displayResult: String

    data class Idle(
        override val displayExpression: String = "0",
        override val displayResult: String = ""
    ) : CalculatorState

    data class Inputting(
        override val displayExpression: String,
        override val displayResult: String = ""
    ) : CalculatorState

    data class Evaluated(
        override val displayExpression: String,
        override val displayResult: String,
        val lastOperation: String = ""
    ) : CalculatorState
}

/**
 * 计算器输入事件
 */
sealed interface CalculatorInput {
    data class Digit(val value: String) : CalculatorInput
    data class Operator(val op: String) : CalculatorInput
    data object Decimal : CalculatorInput
    data object Equals : CalculatorInput
    data object Clear : CalculatorInput
    data object ClearEntry : CalculatorInput
    data object Backspace : CalculatorInput
    data object Percent : CalculatorInput
    data object Reciprocal : CalculatorInput
    data object Square : CalculatorInput
    data object SquareRoot : CalculatorInput
    data object Negate : CalculatorInput

    // Memory operations
    data object MemoryClear : CalculatorInput
    data object MemoryRecall : CalculatorInput
    data object MemoryStore : CalculatorInput
    data object MemoryAdd : CalculatorInput
    data object MemorySubtract : CalculatorInput
}
