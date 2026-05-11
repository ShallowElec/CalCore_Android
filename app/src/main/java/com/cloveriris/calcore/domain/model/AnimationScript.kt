package com.cloveriris.calcore.domain.model

/**
 * 动画脚本 —— 由 VisualizationViewModel 根据 AnimationEvent 生成
 *
 * 包含按时间排序的视觉动作序列，驱动 Canvas 组件状态变化。
 */
data class AnimationScript(
    val events: List<TimedAction> = emptyList(),
    val totalDurationMs: Long = 0L
)

/**
 * 带时间戳的视觉动作
 */
data class TimedAction(
    val timeMs: Long,
    val action: AnimationAction
)

/**
 * 单一视觉动作
 */
sealed class AnimationAction {
    /** 更新位格（64-bit） */
    data class UpdateBitGrid(
        val bits: List<Boolean>,
        val label: String = ""
    ) : AnimationAction()

    /** 更新寄存器值 */
    data class UpdateRegister(
        val index: Int,
        val value: Long,
        val isHighlighted: Boolean = false
    ) : AnimationAction()

    /** 写入内存单元 */
    data class WriteMemory(
        val address: Int,
        val value: Byte,
        val isAllocated: Boolean = true
    ) : AnimationAction()

    /** 压栈 */
    data class PushStack(
        val label: String,
        val address: String = ""
    ) : AnimationAction()

    /** 弹栈 */
    data object PopStack : AnimationAction()

    /** 高亮表达式片段 */
    data class HighlightExpression(
        val startIndex: Int,
        val endIndex: Int
    ) : AnimationAction()

    /** 展示 AST 节点 */
    data class ShowAstNode(
        val nodeType: String,
        val displayValue: String
    ) : AnimationAction()

    /** 清除所有状态 */
    data object ClearAll : AnimationAction()
}
