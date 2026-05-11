package com.cloveriris.calcore.domain.model

/**
 * 计算器核心状态机
 *
 * - [Idle]: 初始状态，等待输入
 * - [Inputting]: 正在输入表达式
 * - [Evaluated]: 已求值，显示结果
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
