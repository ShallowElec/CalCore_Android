package com.cloveriris.calcore.domain.model

/**
 * 模拟的处理器架构抽象
 *
 * 差异体现在：
 * - 寄存器命名
 * - 栈生长方向（目前三种主流架构均为向下生长）
 * - 栈指针 / 帧指针寄存器名
 * - 寻址方式与指令助记符前缀
 */
enum class Architecture(
    val displayName: String,
    val registerNames: List<String>,
    val spRegisterName: String,
    val fpRegisterName: String,
    val stackGrowsDown: Boolean,
    val addressingMode: AddressingMode,
    val mnemonicPrefix: String
) {
    X86_64(
        displayName = "x86-64",
        registerNames = listOf("RAX", "RBX", "RCX", "RDX", "RSI", "RDI", "RSP", "RBP"),
        spRegisterName = "RSP",
        fpRegisterName = "RBP",
        stackGrowsDown = true,
        addressingMode = AddressingMode.SEGMENT_OFFSET,
        mnemonicPrefix = ""
    ),
    ARM64(
        displayName = "ARM64",
        registerNames = listOf("X0", "X1", "X2", "X3", "X4", "X5", "X6", "X7"),
        spRegisterName = "SP",
        fpRegisterName = "X29",
        stackGrowsDown = true,
        addressingMode = AddressingMode.BASE_INDEX_OFFSET,
        mnemonicPrefix = ""
    ),
    RISC_V(
        displayName = "RISC-V",
        registerNames = listOf("A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7"),
        spRegisterName = "SP",
        fpRegisterName = "S0",
        stackGrowsDown = true,
        addressingMode = AddressingMode.BASE_INDEX_OFFSET,
        mnemonicPrefix = ""
    )
}

enum class AddressingMode {
    /** x86 风格的段选择子 + 偏移量 */
    SEGMENT_OFFSET,
    /** ARM/RISC-V 风格的基址寄存器 + 索引寄存器 + 偏移量 */
    BASE_INDEX_OFFSET
}
