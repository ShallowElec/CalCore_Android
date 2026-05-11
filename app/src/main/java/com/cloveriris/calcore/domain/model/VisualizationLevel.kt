package com.cloveriris.calcore.domain.model

/**
 * 可视化内容层级（L1-L8）
 *
 * 用户可通过底部控制条开关各层级的显示。
 */
enum class VisualizationLevel(val displayName: String, val shortName: String) {
    L1_BOOLEAN_ALGEBRA("布尔代数", "L1"),
    L2_NUMERIC_REPRESENTATION("数值表示", "L2"),
    L3_REGISTERS_ALU("寄存器与ALU", "L3"),
    L4_MEMORY_LAYOUT("内存布局", "L4"),
    L5_DATA_STRUCTURES("数据结构", "L5"),
    L6_POINTERS_ADDRESSING("指针与寻址", "L6"),
    L7_INSTRUCTION_SET("指令集", "L7"),
    L8_RESULT_DISPLAY("结果回显", "L8")
}
