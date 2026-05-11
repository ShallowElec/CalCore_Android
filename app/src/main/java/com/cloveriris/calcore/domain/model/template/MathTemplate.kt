package com.cloveriris.calcore.domain.model.template

/**
 * 数学模板系统
 *
 * 模板 = 预定义数学结构 + 参数插槽 + 默认可视化配置。
 * 降低用户输入门槛，统一各模块的交互范式。
 */

sealed interface MathTemplate {
    val id: String
    val category: TemplateCategory
    val name: String
    val description: String
    val parameters: List<TemplateParameter>
    val defaultExpressions: List<String>
    val recommendedViewport: ViewportConfig?
}

enum class TemplateCategory {
    GRAPHING,
    LINEAR_ALGEBRA,
    CALCULUS,
    ORDINARY_DIFF_EQ,
    PARTIAL_DIFF_EQ
}

data class TemplateParameter(
    val name: String,
    val symbol: String,
    val defaultValue: Double,
    val range: ClosedFloatingPointRange<Double>,
    val step: Double = 0.01
)

data class ViewportConfig(
    val xRange: ClosedFloatingPointRange<Double> = -10.0..10.0,
    val yRange: ClosedFloatingPointRange<Double> = -10.0..10.0,
    val zRange: ClosedFloatingPointRange<Double>? = null,
    val coordinateSystem: CoordinateSystem = CoordinateSystem.CARTESIAN_2D
)

enum class CoordinateSystem {
    CARTESIAN_2D,
    CARTESIAN_3D,
    POLAR,
    CYLINDRICAL,
    SPHERICAL
}

// ==================== 预置模板实现 ====================

/** 图形模式：抛物线 */
data class ParabolaTemplate(
    override val id: String = "graphing.parabola",
    override val category: TemplateCategory = TemplateCategory.GRAPHING,
    override val name: String = "抛物线",
    override val description: String = "y = ax² + bx + c",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("a", "a", 1.0, -5.0..5.0),
        TemplateParameter("b", "b", 0.0, -5.0..5.0),
        TemplateParameter("c", "c", 0.0, -5.0..5.0)
    ),
    override val defaultExpressions: List<String> = listOf("a*x^2 + b*x + c"),
    override val recommendedViewport: ViewportConfig = ViewportConfig(-5.0..5.0, -5.0..5.0)
) : MathTemplate

/** 图形模式：李萨如曲线 */
data class LissajousTemplate(
    override val id: String = "graphing.lissajous",
    override val category: TemplateCategory = TemplateCategory.GRAPHING,
    override val name: String = "李萨如曲线",
    override val description: String = "参数曲线 (A·sin(a·t+δ), B·sin(b·t))",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("A", "A", 1.0, 0.1..3.0),
        TemplateParameter("B", "B", 1.0, 0.1..3.0),
        TemplateParameter("a", "a", 3.0, 1.0..10.0),
        TemplateParameter("b", "b", 2.0, 1.0..10.0),
        TemplateParameter("delta", "δ", 0.0, 0.0..6.28, 0.1)
    ),
    override val defaultExpressions: List<String> = listOf(
        "(A*sin(a*t+delta), B*sin(b*t))"
    ),
    override val recommendedViewport: ViewportConfig = ViewportConfig(-3.0..3.0, -3.0..3.0)
) : MathTemplate

/** 线性代数：二维旋转矩阵 */
data class Rotation2DTemplate(
    override val id: String = "linalg.rotation_2d",
    override val category: TemplateCategory = TemplateCategory.LINEAR_ALGEBRA,
    override val name: String = "二维旋转",
    override val description: String = "旋转矩阵对标准基向量的变换",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("theta", "θ", 0.785, 0.0..6.28, 0.05)
    ),
    override val defaultExpressions: List<String> = listOf(
        "[[cos(theta), -sin(theta)], [sin(theta), cos(theta)]]"
    ),
    override val recommendedViewport: ViewportConfig = ViewportConfig(-2.0..2.0, -2.0..2.0)
) : MathTemplate

/** 微积分：黎曼和 */
data class RiemannSumTemplate(
    override val id: String = "calculus.riemann",
    override val category: TemplateCategory = TemplateCategory.CALCULUS,
    override val name: String = "黎曼和",
    override val description: String = "用矩形面积逼近定积分",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("a", "a", 0.0, -10.0..10.0),
        TemplateParameter("b", "b", 5.0, -10.0..10.0),
        TemplateParameter("n", "n", 10.0, 1.0..200.0, 1.0)
    ),
    override val defaultExpressions: List<String> = listOf("x^2"),
    override val recommendedViewport: ViewportConfig = ViewportConfig(-1.0..6.0, -1.0..26.0)
) : MathTemplate

/** 微积分：泰勒展开 */
data class TaylorExpansionTemplate(
    override val id: String = "calculus.taylor",
    override val category: TemplateCategory = TemplateCategory.CALCULUS,
    override val name: String = "泰勒展开",
    override val description: String = "n 阶泰勒多项式逼近原函数",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("x0", "x₀", 0.0, -5.0..5.0),
        TemplateParameter("n", "n", 5.0, 0.0..20.0, 1.0)
    ),
    override val defaultExpressions: List<String> = listOf("sin(x)"),
    override val recommendedViewport: ViewportConfig = ViewportConfig(-5.0..5.0, -2.0..2.0)
) : MathTemplate

/** 微分方程：指数衰减 */
data class ExponentialDecayTemplate(
    override val id: String = "ode.decay",
    override val category: TemplateCategory = TemplateCategory.ORDINARY_DIFF_EQ,
    override val name: String = "指数衰减",
    override val description: String = "dy/dt = -λy, y(0)=y₀",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("y0", "y₀", 1.0, 0.0..5.0),
        TemplateParameter("lambda", "λ", 0.5, 0.01..2.0)
    ),
    override val defaultExpressions: List<String> = listOf("dy/dt = -lambda*y"),
    override val recommendedViewport: ViewportConfig = ViewportConfig(0.0..10.0, 0.0..2.0)
) : MathTemplate

/** 微分方程：简谐振动 */
data class HarmonicOscillatorTemplate(
    override val id: String = "ode.harmonic",
    override val category: TemplateCategory = TemplateCategory.ORDINARY_DIFF_EQ,
    override val name: String = "简谐振动",
    override val description: String = "d²x/dt² + ω²x = 0",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("omega", "ω", 1.0, 0.1..5.0),
        TemplateParameter("A", "A", 1.0, 0.1..3.0),
        TemplateParameter("phi", "φ", 0.0, 0.0..6.28, 0.05)
    ),
    override val defaultExpressions: List<String> = listOf("d2x/dt2 + omega^2*x = 0"),
    override val recommendedViewport: ViewportConfig = ViewportConfig(-3.0..3.0, -3.0..3.0)
) : MathTemplate

/** 微分方程：捕食者-猎物模型 */
data class LotkaVolterraTemplate(
    override val id: String = "ode.lotka_volterra",
    override val category: TemplateCategory = TemplateCategory.ORDINARY_DIFF_EQ,
    override val name: String = "捕食者-猎物",
    override val description: String = "Lotka-Volterra 模型",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("alpha", "α", 1.0, 0.1..3.0),
        TemplateParameter("beta", "β", 0.5, 0.01..2.0),
        TemplateParameter("gamma", "γ", 0.5, 0.01..2.0),
        TemplateParameter("delta", "δ", 1.0, 0.1..3.0)
    ),
    override val defaultExpressions: List<String> = listOf(
        "dx/dt = alpha*x - beta*x*y",
        "dy/dt = delta*x*y - gamma*y"
    ),
    override val recommendedViewport: ViewportConfig = ViewportConfig(0.0..5.0, 0.0..5.0)
) : MathTemplate

/** PDE：一维热方程 */
data class HeatEquation1DTemplate(
    override val id: String = "pde.heat_1d",
    override val category: TemplateCategory = TemplateCategory.PARTIAL_DIFF_EQ,
    override val name: String = "一维热方程",
    override val description: String = "∂u/∂t = α·∂²u/∂x²",
    override val parameters: List<TemplateParameter> = listOf(
        TemplateParameter("alpha", "α", 0.1, 0.01..1.0),
        TemplateParameter("L", "L", 1.0, 0.1..5.0)
    ),
    override val defaultExpressions: List<String> = listOf("u(x,0) = sin(pi*x/L)"),
    override val recommendedViewport: ViewportConfig? = null
) : MathTemplate

/**
 * 模板库（硬编码的预置模板集合）
 */
object TemplateLibrary {
    val all: List<MathTemplate> = listOf(
        ParabolaTemplate(),
        LissajousTemplate(),
        Rotation2DTemplate(),
        RiemannSumTemplate(),
        TaylorExpansionTemplate(),
        ExponentialDecayTemplate(),
        HarmonicOscillatorTemplate(),
        LotkaVolterraTemplate(),
        HeatEquation1DTemplate()
    )

    fun byCategory(category: TemplateCategory): List<MathTemplate> =
        all.filter { it.category == category }

    fun byId(id: String): MathTemplate? =
        all.find { it.id == id }
}
