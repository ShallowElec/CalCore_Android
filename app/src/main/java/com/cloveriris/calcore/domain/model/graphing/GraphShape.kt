package com.cloveriris.calcore.domain.model.graphing

import androidx.compose.ui.graphics.Color

/**
 * 图形模式渲染的形状基类
 *
 * 所有坐标均为世界坐标（数学坐标系），由 Canvas 负责变换到屏幕坐标。
 */
sealed class GraphShape {
    abstract val color: Color

    /**
     * 折线（用于显函数、参数曲线等）
     */
    data class Polyline(
        val points: List<GraphPoint>,
        override val color: Color,
        val strokeWidth: Float = 2f
    ) : GraphShape()

    /**
     * 点集（用于离散点、采样点标记）
     */
    data class Points(
        val points: List<GraphPoint>,
        override val color: Color,
        val radius: Float = 3f
    ) : GraphShape()
}

/**
 * 世界坐标系中的点
 */
data class GraphPoint(val x: Double, val y: Double)
