package com.cloveriris.calcore.domain.model

/**
 * 模拟的处理器架构抽象
 */
enum class Architecture(val displayName: String, val registerNames: List<String>) {
    X86_64(
        displayName = "x86-64",
        registerNames = listOf("RAX", "RBX", "RCX", "RDX", "RSI", "RDI", "RSP", "RBP")
    ),
    ARM64(
        displayName = "ARM64",
        registerNames = listOf("X0", "X1", "X2", "X3", "X4", "X5", "X6", "X7")
    ),
    RISC_V(
        displayName = "RISC-V",
        registerNames = listOf("A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7")
    )
}
