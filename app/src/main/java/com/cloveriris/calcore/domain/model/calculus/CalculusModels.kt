package com.cloveriris.calcore.domain.model.calculus

/**
 * 微积分可视化的步骤与结果模型
 *
 * 封装数值结果 + 几何中间状态，驱动 Canvas 动画。
 */

/**
 * 微积分计算结果
 */
data class CalculusResult(
    val value: Double,
    val steps: List<CalculusStep>,
    val geometryFrames: List<GeometryFrame> = emptyList()
)

/**
 * 单个计算步骤（右侧步骤面板用）
 */
data class CalculusStep(
    val title: String,
    val formula: String,
    val explanation: String,
    val numericResult: Double? = null
)

/**
 * 几何动画帧（中央 Canvas 用）
 *
 * 每一帧描述一个离散时间点的几何对象集合。
 */
data class GeometryFrame(
    val timestamp: Double,           // 归一化时间 [0, 1]
    val shapes: List<DrawableShape>,
    val labels: List<Label> = emptyList()
)

/**
 * 可绘制几何对象（Compose Canvas 的抽象表示）
 */
sealed class DrawableShape {
    data class Point(
        val x: Double,
        val y: Double,
        val radius: Float = 4f,
        val color: Int = 0xFF00FF41.toInt()
    ) : DrawableShape()

    data class Line(
        val x1: Double, val y1: Double,
        val x2: Double, val y2: Double,
        val strokeWidth: Float = 2f,
        val color: Int = 0xFF00FF41.toInt(),
        val isDashed: Boolean = false
    ) : DrawableShape()

    data class Rect(
        val x: Double, val y: Double,
        val width: Double, val height: Double,
        val fillColor: Int = 0x2200FF41,
        val strokeColor: Int = 0xFF00FF41.toInt()
    ) : DrawableShape()

    data class Circle(
        val cx: Double, val cy: Double,
        val radius: Double,
        val fillColor: Int? = null,
        val strokeColor: Int = 0xFF00FF41.toInt(),
        val strokeWidth: Float = 2f
    ) : DrawableShape()

    data class Path(
        val points: List<Pair<Double, Double>>,
        val strokeWidth: Float = 2f,
        val color: Int = 0xFF00FF41.toInt()
    ) : DrawableShape()

    data class Arrow(
        val x1: Double, val y1: Double,
        val x2: Double, val y2: Double,
        val color: Int = 0xFF00FF41.toInt()
    ) : DrawableShape()
}

data class Label(
    val text: String,
    val x: Double,
    val y: Double,
    val color: Int = 0xFF8B949E.toInt()
)

/**
 * 数值积分配置
 */
data class IntegrationConfig(
    val function: String,           // 如 "x^2"
    val lowerBound: Double,
    val upperBound: Double,
    val method: IntegrationMethod,
    val subdivisions: Int
)

enum class IntegrationMethod {
    LEFT_RIEMANN,
    RIGHT_RIEMANN,
    MIDPOINT,
    TRAPEZOID,
    SIMPSON
}

/**
 * 泰勒展开配置
 */
data class TaylorConfig(
    val function: String,
    val center: Double,
    val order: Int
)
