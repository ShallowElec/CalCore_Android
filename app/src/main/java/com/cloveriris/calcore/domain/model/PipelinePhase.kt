package com.cloveriris.calcore.domain.model

/**
 * 可视化流水线阶段
 *
 * 将 L1-L8 按真实计算流程映射为 6 个阶段。
 * 按下 = 后，可视化像 CPU 流水线一样从上到下逐阶段执行。
 */
enum class PipelinePhase(val displayName: String, val shortName: String, val order: Int) {
    IDLE("等待", "IDLE", 0),
    PHASE_INPUT("输入 / 数值", "INPUT", 1),           // L1-L2
    PHASE_REGISTERS("寄存器装载", "REG", 2),          // L3
    PHASE_MEMORY("内存布局", "MEM", 3),               // L4
    PHASE_PARSE("表达式解析", "PARSE", 4),            // L5
    PHASE_EXECUTE("执行与寻址", "EXEC", 5),           // L6-L7
    PHASE_OUTPUT("结果回显", "OUT", 6);               // L8

    companion object {
        fun orderedValues(): List<PipelinePhase> =
            entries.filter { it != IDLE }.sortedBy { it.order }
    }
}

/**
 * 阶段快照 —— 记录已完成阶段的一句话摘要
 */
data class PhaseSnapshot(
    val phase: PipelinePhase,
    val summary: String,
    val timestampMs: Long = 0L
)

/**
 * 阶段到可视化层级的映射
 */
fun PipelinePhase.toVisualizationLevels(): Set<VisualizationLevel> = when (this) {
    PipelinePhase.PHASE_INPUT -> setOf(
        VisualizationLevel.L1_BOOLEAN_ALGEBRA,
        VisualizationLevel.L2_NUMERIC_REPRESENTATION
    )
    PipelinePhase.PHASE_REGISTERS -> setOf(VisualizationLevel.L3_REGISTERS_ALU)
    PipelinePhase.PHASE_MEMORY -> setOf(VisualizationLevel.L4_MEMORY_LAYOUT)
    PipelinePhase.PHASE_PARSE -> setOf(VisualizationLevel.L5_DATA_STRUCTURES)
    PipelinePhase.PHASE_EXECUTE -> setOf(
        VisualizationLevel.L6_POINTERS_ADDRESSING,
        VisualizationLevel.L7_INSTRUCTION_SET
    )
    PipelinePhase.PHASE_OUTPUT -> setOf(VisualizationLevel.L8_RESULT_DISPLAY)
    PipelinePhase.IDLE -> emptySet()
}
