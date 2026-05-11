package com.cloveriris.calcore.domain.model

/**
 * 计算历史记录条目
 */
data class HistoryEntry(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
