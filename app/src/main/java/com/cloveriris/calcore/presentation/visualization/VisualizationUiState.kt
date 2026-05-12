package com.cloveriris.calcore.presentation.visualization

import com.cloveriris.calcore.domain.model.AnimationAction
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.PipelinePhase
import com.cloveriris.calcore.domain.model.VisualizationLevel
import com.cloveriris.calcore.engine.parser.Expression
import com.cloveriris.calcore.ui.visualization.MemoryCellVisual
import com.cloveriris.calcore.ui.visualization.RegisterVisual
import com.cloveriris.calcore.ui.visualization.StackFrameVisual

data class LinkedListNodeVisual(
    val id: String,
    val value: String,
    val nextId: String?,
    val isNew: Boolean = false,
    val isHighlighted: Boolean = false,
    val isPointer: Boolean = false
)

data class PipelineStageVisual(
    val stageName: String,
    val instruction: String = "",
    val isActive: Boolean = false,
    val progress: Float = 0f
)

data class DisplayBufferVisual(
    val text: String = "",
    val cursorPosition: Int = 0,
    val isTyping: Boolean = false
)

data class LogicGateState(
    val gateType: AnimationAction.LogicGateType = AnimationAction.LogicGateType.AND,
    val leftBits: List<Boolean> = emptyList(),
    val rightBits: List<Boolean> = emptyList(),
    val resultBits: List<Boolean> = emptyList(),
    val signalProgress: Float = 0f
)

data class AluOperationVisual(
    val operation: String = "",
    val left: Long = 0L,
    val right: Long = 0L,
    val result: Long = 0L,
    val isActive: Boolean = false
)

data class DataPathVisual(
    val from: String = "",
    val to: String = "",
    val progress: Float = 0f
)

data class AddressBusVisual(
    val segment: Long = 0L,
    val offset: Long = 0L,
    val fullAddress: Long = 0L,
    val progress: Float = 0f
)

/** 栈帧动画状态（PUSH / POP 滑入滑出） */
data class StackAnimationState(
    val operation: AnimationAction.StackOperationType = AnimationAction.StackOperationType.NONE,
    val progress: Float = 0f,
    val frameLabel: String = "",
    val frameValue: String = "",
    val registerName: String = ""
)

/** 链表连线动画状态 */
data class LinkedListWireState(
    val fromNodeId: String = "",
    val toNodeId: String = "",
    val progress: Float = 0f
)

/** 内存指针连线动画状态 */
data class MemoryPointerAnimationState(
    val sourceAddress: Int = 0,
    val targetAddress: Int = 0,
    val progress: Float = 0f
)

data class VisualizationUiState(
    val architecture: Architecture = Architecture.X86_64,
    val playbackSpeed: Float = 1.0f, // 0.2x ~ 2.0x
    val currentAst: Expression? = null,
    val astGrowthProgress: Float = 1.0f,
    val operatorStack: List<String> = emptyList(),
    val linkedListNodes: List<LinkedListNodeVisual> = emptyList(),
    val linkedListWire: LinkedListWireState = LinkedListWireState(),
    val instructionPipeline: List<PipelineStageVisual> = emptyList(),
    val displayBuffer: DisplayBufferVisual? = null,
    val resultFlowProgress: Float = 0f,
    val logicGateState: LogicGateState = LogicGateState(),
    val aluOperation: AluOperationVisual? = null,
    val dataPath: DataPathVisual? = null,
    val addressBus: AddressBusVisual? = null,
    val cursorMemoryAddress: Int = 0,
    val previousCursorAddress: Int = 0,
    val bitGridBits: List<Boolean> = List(64) { false },
    val bitGridLabel: String = "IDLE",
    val bitGridHighlights: Set<Int> = emptySet(),
    val registers: List<RegisterVisual> = emptyList(),
    val memoryCells: List<MemoryCellVisual> = emptyList(),
    val memoryPointerAnimation: MemoryPointerAnimationState = MemoryPointerAnimationState(),
    val stackFrames: List<StackFrameVisual> = emptyList(),
    val stackAnimation: StackAnimationState = StackAnimationState(),
    val stackPointerValue: String = "",
    val stackPointerHighlighted: Boolean = false,
    val activeLevels: Set<VisualizationLevel> = VisualizationLevel.entries.toSet(),
    val isPanelExpanded: Boolean = false,
    val lastEventDescription: String = "",
    val evaluationProgress: Float = 0f,
    val isPlaying: Boolean = false,
    val totalDurationMs: Long = 0L,
    val currentTimeMs: Long = 0L,
    // ===== 阶段式流水线状态 =====
    val activePhase: PipelinePhase? = null,
    val phaseProgress: Float = 0f,
    val completedPhases: Set<PipelinePhase> = emptySet(),
    val phaseSnapshots: Map<PipelinePhase, String> = emptyMap(),
    val phaseDurations: Map<PipelinePhase, Long> = emptyMap(),
)
