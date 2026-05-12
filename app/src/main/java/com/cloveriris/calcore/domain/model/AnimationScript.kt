package com.cloveriris.calcore.domain.model

import com.cloveriris.calcore.engine.parser.Expression

/**
 * 动画脚本 —— 由 VisualizationEngine 根据 AnimationEvent 生成
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
 *
 * 每个动作都是对 VisualizationUiState 的不可变变换指令。
 */
sealed class AnimationAction {

    // ==================== 基础动作（已有）====================

    /** 更新位格（64-bit） */
    data class UpdateBitGrid(
        val bits: List<Boolean>,
        val label: String = ""
    ) : AnimationAction()

    /** 更新位格高亮区域（L2: 符号位/指数位/尾数位） */
    data class UpdateBitGridHighlights(
        val highlightIndices: Set<Int> = emptySet(),
        val highlightColor: Int = 0xFFFFA657.toInt() // TerminalAmber
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
        val isAllocated: Boolean = true,
        val isPointer: Boolean = false,
        val isWriting: Boolean = false, // 正在写入动画中
        val regionTag: String = ""      // STACK / HEAP / DATA / CONST
    ) : AnimationAction()

    /** 内存指针连线生长动画 */
    data class AnimateMemoryPointer(
        val sourceAddress: Int = 0,
        val targetAddress: Int = 0,
        val progress: Float = 0f // 0.0 ~ 1.0
    ) : AnimationAction()

    /** 压栈 */
    data class PushStack(
        val label: String,
        val address: String = ""
    ) : AnimationAction()

    /** 弹栈 */
    data object PopStack : AnimationAction()

    /** 更新栈指针值与高亮 */
    data class UpdateStackPointer(
        val value: String = "",
        val isHighlighted: Boolean = false
    ) : AnimationAction()

    /** 栈帧动画状态（PUSH/POP 滑入/滑出） */
    data class UpdateStackAnimation(
        val operation: StackOperationType,
        val progress: Float = 0f, // 0.0 ~ 1.0
        val frameLabel: String = "",
        val frameValue: String = "",
        val registerName: String = ""
    ) : AnimationAction()

    enum class StackOperationType { NONE, PUSH, POP }

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

    /** AST 生长进度 */
    data class UpdateAstGrowth(
        val ast: Expression?,
        val progress: Float // 0.0 ~ 1.0
    ) : AnimationAction()

    /** 清除所有状态 */
    data object ClearAll : AnimationAction()

    // ==================== L1 布尔代数 ====================

    /** 更新逻辑门网格状态 */
    data class UpdateLogicGates(
        val gateType: LogicGateType,
        val leftBits: List<Boolean> = emptyList(),
        val rightBits: List<Boolean> = emptyList(),
        val resultBits: List<Boolean> = emptyList(),
        val signalProgress: Float = 0f // 0.0~1.0 信号流动进度
    ) : AnimationAction()

    enum class LogicGateType { AND, OR, XOR, NOT }

    // ==================== L3 ALU & 数据路径 ====================

    /** 更新 ALU 运算可视化 */
    data class UpdateAluOperation(
        val operation: String, // "ADD", "SUB", "MUL", "DIV", "AND"...
        val leftOperand: Long = 0L,
        val rightOperand: Long = 0L,
        val result: Long = 0L,
        val isActive: Boolean = false
    ) : AnimationAction()

    /** 更新数据搬运路径 */
    data class UpdateDataPath(
        val fromRegister: String = "",
        val toRegister: String = "",
        val progress: Float = 0f // 0.0~1.0
    ) : AnimationAction()

    // ==================== L5 数据结构 ====================

    /** 更新运算符栈 */
    data class UpdateOperatorStack(
        val stack: List<String> = emptyList(),
        val pushLabel: String? = null,
        val popCount: Int = 0
    ) : AnimationAction()

    /** 更新链表节点 */
    data class UpdateLinkedList(
        val nodes: List<LinkedListNodeData> = emptyList()
    ) : AnimationAction()

    data class LinkedListNodeData(
        val id: String,
        val value: String,
        val nextId: String? = null,
        val isNew: Boolean = false,
        val isHighlighted: Boolean = false,
        val isPointer: Boolean = false // 绿色空心方块语义
    )

    /** 链表连线生长动画 */
    data class AnimateLinkedListWire(
        val fromNodeId: String = "",
        val toNodeId: String = "",
        val progress: Float = 0f // 0.0 ~ 1.0
    ) : AnimationAction()

    // ==================== L6 指针与寻址 ====================

    /** 更新地址总线 */
    data class UpdateAddressBus(
        val segment: Long = 0L,
        val offset: Long = 0L,
        val fullAddress: Long = 0L,
        val progress: Float = 0f // 拼接进度 0.0~1.0
    ) : AnimationAction()

    /** 更新光标内存地址 */
    data class UpdateCursorAddress(
        val address: Int = 0,
        val bufferIndex: Int = 0
    ) : AnimationAction()

    // ==================== L7 指令流水线 ====================

    /** 更新指令流水线 */
    data class UpdateInstructionPipeline(
        val stages: List<PipelineStageData> = emptyList()
    ) : AnimationAction()

    data class PipelineStageData(
        val stageName: String, // "FETCH", "DECODE", "EXECUTE", "WRITEBACK"
        val instruction: String = "",
        val isActive: Boolean = false,
        val progress: Float = 0f
    )

    // ==================== L8 结果回显 ====================

    /** 更新显示缓冲区 */
    data class UpdateDisplayBuffer(
        val text: String = "",
        val cursorPosition: Int = 0,
        val isTyping: Boolean = false
    ) : AnimationAction()

    /** 更新结果数据流进度 */
    data class UpdateResultFlow(
        val fromStage: String = "", // "REGISTER", "MEMORY", "ALU"
        val toStage: String = "",   // "MEMORY", "DISPLAY"
        val progress: Float = 0f    // 0.0~1.0
    ) : AnimationAction()

    // ==================== 阶段控制 ====================

    /** 进入新阶段 */
    data class EnterPhase(
        val phase: PipelinePhase,
        val phaseDurationMs: Long = 0L
    ) : AnimationAction()

    /** 退出当前阶段 */
    data class ExitPhase(
        val phase: PipelinePhase,
        val summary: String = ""
    ) : AnimationAction()

    // ==================== 元数据 ====================

    /** 更新事件描述 */
    data class UpdateDescription(
        val description: String
    ) : AnimationAction()

    /** 设置总时长 */
    data class SetDuration(
        val durationMs: Long
    ) : AnimationAction()
}
