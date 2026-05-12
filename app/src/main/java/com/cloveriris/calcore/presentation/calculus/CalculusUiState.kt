package com.cloveriris.calcore.presentation.calculus

import com.cloveriris.calcore.domain.model.calculus.*

/**
 * 微积分工作台 UI 状态
 */
data class CalculusUiState(
    val mode: CalculusMode = CalculusMode.DERIVATIVE,
    val function: String = "x^2",
    val point: Double = 1.0,
    val lowerBound: Double = 0.0,
    val upperBound: Double = 2.0,
    val center: Double = 0.0,
    val order: Int = 3,
    val subdivisions: Int = 10,
    val integrationMethod: IntegrationMethod = IntegrationMethod.LEFT_RIEMANN,
    val steps: List<CalculusStep> = emptyList(),
    val frames: List<GeometryFrame> = emptyList(),
    val currentFrameIndex: Int = 0,
    val isPlaying: Boolean = false,
    val resultValue: Double? = null,
    val viewport: CalculusViewport = CalculusViewport()
)

enum class CalculusMode(val displayName: String) {
    LIMIT("极限"),
    DERIVATIVE("导数"),
    INTEGRAL("积分"),
    TAYLOR("泰勒展开")
}

/**
 * 微积分画布视口（世界坐标系）
 */
data class CalculusViewport(
    val minX: Double = -5.0,
    val maxX: Double = 5.0,
    val minY: Double = -3.0,
    val maxY: Double = 3.0
)
