package com.cloveriris.calcore.domain.model

/**
 * 程序员模式进制
 */
enum class NumberBase(val radix: Int, val displayName: String, val prefix: String) {
    BIN(2, "BIN", "0b"),
    OCT(8, "OCT", "0o"),
    DEC(10, "DEC", ""),
    HEX(16, "HEX", "0x");

    fun validDigits(): String = when (this) {
        BIN -> "01"
        OCT -> "01234567"
        DEC -> "0123456789"
        HEX -> "0123456789ABCDEF"
    }

    fun isValidDigit(char: Char): Boolean =
        char.uppercaseChar() in validDigits()
}
