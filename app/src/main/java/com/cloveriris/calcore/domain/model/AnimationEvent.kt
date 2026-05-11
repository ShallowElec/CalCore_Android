package com.cloveriris.calcore.domain.model

/**
 * 用户按键触发的动画事件
 *
 * CalculatorViewModel 在每次输入时生成对应事件，
 * 通过 SharedFlow 传递给 VisualizationViewModel。
 */
sealed class AnimationEvent {
    /** 数字键输入 */
    data class DigitEntered(val digit: Char, val currentExpression: String) : AnimationEvent()

    /** 运算符输入 */
    data class OperatorEntered(val operator: String, val currentExpression: String) : AnimationEvent()

    /** 等号执行 */
    data class Evaluated(val expression: String, val result: Double) : AnimationEvent()

    /** 清除 */
    data object Clear : AnimationEvent()

    /** 退格 */
    data object Backspace : AnimationEvent()

    /** 内存操作 */
    data class MemoryOperation(val type: MemoryOpType) : AnimationEvent()

    /** 小数点 */
    data object DecimalEntered : AnimationEvent()

    /** 表达式已解析（等号执行时触发 AST 可视化） */
    data class ExpressionParsed(val expression: String) : AnimationEvent()

    /** 位运算（程序员模式） */
    data class BitOperation(
        val op: String,
        val left: Long,
        val right: Long,
        val result: Long
    ) : AnimationEvent()
}

enum class MemoryOpType {
    STORE, RECALL, ADD, SUBTRACT, CLEAR
}
