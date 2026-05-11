package com.cloveriris.calcore.domain.model

/**
 * 程序员模式位宽
 */
enum class BitWidth(val bits: Int, val displayName: String, val mask: Long) {
    BYTE(8, "BYTE", 0xFFL),
    WORD(16, "WORD", 0xFFFFL),
    DWORD(32, "DWORD", 0xFFFFFFFFL),
    QWORD(64, "QWORD", -1L);

    /**
     * 将值截断到当前位宽
     */
    fun truncate(value: Long): Long = value and mask
}
