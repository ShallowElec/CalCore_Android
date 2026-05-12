package com.cloveriris.calcore.engine.calculus

import com.cloveriris.calcore.domain.model.calculus.*
import com.cloveriris.calcore.engine.evaluator.Evaluator
import com.cloveriris.calcore.engine.parser.Lexer
import com.cloveriris.calcore.engine.parser.Parser
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 数值微积分引擎
 *
 * 提供导数近似、数值积分、泰勒展开等计算的逐步结果。
 */
object NumericalCalculus {

    /**
     * 数值积分（带几何帧生成）—— 黎曼和累加动画
     *
     * 每帧保留所有之前生成的矩形/梯形，当前新增元素高亮，
     * 形成从左到右逐个累加的视觉效果。
     */
    fun integrate(config: IntegrationConfig): CalculusResult {
        val f = compileFunction(config.function)
        val a = config.lowerBound
        val b = config.upperBound
        val n = config.subdivisions.coerceAtLeast(1)
        val dx = (b - a) / n

        val steps = mutableListOf<CalculusStep>()
        val frames = mutableListOf<GeometryFrame>()
        var sum = 0.0
        val accumulatedShapes = mutableListOf<DrawableShape>()

        steps.add(CalculusStep(
            title = "区间分割",
            formula = "Δx = (b-a)/n = ${String.format("%.4f", dx)}",
            explanation = "将 [${String.format("%.2f", a)}, ${String.format("%.2f", b)}] 均分为 $n 个子区间"
        ))

        // 函数曲线（作为背景，始终显示）
        val curvePoints = (0..100).map { i ->
            val xv = a + (b - a) * i / 100.0
            xv to f(xv)
        }
        val curveShape = DrawableShape.Path(curvePoints, strokeWidth = 2f, color = 0xFF8B949E.toInt())

        for (i in 0 until n) {
            val x0 = a + i * dx
            val x1 = x0 + dx
            val height = when (config.method) {
                IntegrationMethod.LEFT_RIEMANN -> f(x0)
                IntegrationMethod.RIGHT_RIEMANN -> f(x1)
                IntegrationMethod.MIDPOINT -> f((x0 + x1) / 2)
                IntegrationMethod.TRAPEZOID -> (f(x0) + f(x1)) / 2
                IntegrationMethod.SIMPSON -> {
                    val xm = (x0 + x1) / 2
                    (f(x0) + 4 * f(xm) + f(x1)) / 6
                }
            }
            val area = height * dx
            sum += area

            // 新增的几何形状（当前高亮）
            val newShapes = mutableListOf<DrawableShape>()
            when (config.method) {
                IntegrationMethod.LEFT_RIEMANN, IntegrationMethod.RIGHT_RIEMANN,
                IntegrationMethod.MIDPOINT -> {
                    // 当前矩形：高亮绿色
                    newShapes.add(DrawableShape.Rect(
                        x = x0, y = 0.0,
                        width = dx, height = height,
                        fillColor = 0x4400FF41, strokeColor = 0xFF00FF41.toInt()
                    ))
                    // 已累积的矩形：暗淡绿色
                    accumulatedShapes.add(DrawableShape.Rect(
                        x = x0, y = 0.0,
                        width = dx, height = height,
                        fillColor = 0x1800FF41, strokeColor = 0x6600FF41
                    ))
                }
                IntegrationMethod.TRAPEZOID -> {
                    newShapes.add(DrawableShape.Path(listOf(
                        x0 to 0.0, x0 to f(x0), x1 to f(x1), x1 to 0.0, x0 to 0.0
                    ), strokeWidth = 2f, color = 0xFF00FF41.toInt()))
                    accumulatedShapes.add(DrawableShape.Path(listOf(
                        x0 to 0.0, x0 to f(x0), x1 to f(x1), x1 to 0.0, x0 to 0.0
                    ), strokeWidth = 1f, color = 0x448B949E.toInt()))
                }
                IntegrationMethod.SIMPSON -> {
                    newShapes.add(DrawableShape.Path(listOf(
                        x0 to 0.0, x0 to f(x0), x1 to f(x1), x1 to 0.0, x0 to 0.0
                    ), strokeWidth = 2f, color = 0xFF00FF41.toInt()))
                    accumulatedShapes.add(DrawableShape.Path(listOf(
                        x0 to 0.0, x0 to f(x0), x1 to f(x1), x1 to 0.0, x0 to 0.0
                    ), strokeWidth = 1f, color = 0x448B949E.toInt()))
                }
            }

            val allShapes = mutableListOf<DrawableShape>()
            allShapes.add(curveShape)
            allShapes.addAll(accumulatedShapes)
            allShapes.addAll(newShapes)

            frames.add(GeometryFrame(
                timestamp = (i + 1) / n.toDouble(),
                shapes = allShapes,
                labels = listOf(
                    Label("S≈${String.format("%.4f", sum)}", b, sum * 0.05, 0xFF00FF41.toInt()),
                    Label("+${String.format("%.4f", area)}", x0 + dx/2, height + 0.2, 0xFFFFA657.toInt())
                )
            ))
        }

        // 最终帧：显示所有累积区域 + 真实积分值对比
        val trueIntegral = trueIntegral(config.function, a, b)
        val error = abs(sum - trueIntegral)
        frames.add(GeometryFrame(
            timestamp = 1.0,
            shapes = listOf(curveShape) + accumulatedShapes,
            labels = listOf(
                Label("近似值: ${String.format("%.6f", sum)}", b, sum * 0.05 + 0.3, 0xFF00FF41.toInt()),
                Label("真实值: ${String.format("%.6f", trueIntegral)}", b, sum * 0.05 - 0.3, 0xFF8B949E.toInt()),
                Label("误差: ${String.format("%.6f", error)}", b, sum * 0.05 - 0.6, 0xFFFF5252.toInt())
            )
        ))

        steps.add(CalculusStep(
            title = "累加结果",
            formula = "∫≈ ${String.format("%.6f", sum)}",
            explanation = "${config.method} 法，$n 个子区间，误差 ${String.format("%.6f", error)}",
            numericResult = sum
        ))

        return CalculusResult(sum, steps, frames)
    }

    /**
     * 导数近似：切线滑动 + 曲率圆构造
     *
     * 生成三帧类型：
     * 1. 割线收缩动画（h→0）
     * 2. 切线/法线实时绘制（含可拖拽点提示）
     * 3. 曲率圆构造
     */
    fun derivative(config: DerivativeConfig): DerivativeResult {
        val f = compileFunction(config.function)
        val x = config.point
        val h = 0.5  // 初始步长更大，视觉效果更明显

        val forward = (f(x + 0.01) - f(x)) / 0.01
        val backward = (f(x) - f(x - 0.01)) / 0.01
        val central = (f(x + 0.01) - f(x - 0.01)) / (2 * 0.01)

        val fx = f(x)
        val slope = central

        val frames = mutableListOf<GeometryFrame>()

        // ===== 阶段1：割线收缩动画（h 从大到小） =====
        val steps = 15
        for (i in 1..steps) {
            val hi = h * (steps - i + 1) / steps
            val dy = (f(x + hi) - f(x - hi)) / (2 * hi)
            val shapes = mutableListOf<DrawableShape>()

            // 割线（红色，逐渐变细）
            shapes.add(DrawableShape.Line(
                x1 = x - hi * 2, y1 = fx - dy * hi * 2,
                x2 = x + hi * 2, y2 = fx + dy * hi * 2,
                strokeWidth = (3f * i / steps).coerceAtLeast(1f),
                color = 0xFFFF5252.toInt(),
                isDashed = true
            ))
            // 割线端点
            shapes.add(DrawableShape.Point(x - hi, f(x - hi), radius = 3f, color = 0xFFFFA657.toInt()))
            shapes.add(DrawableShape.Point(x + hi, f(x + hi), radius = 3f, color = 0xFFFFA657.toInt()))
            // 中心点（绿色高亮）
            shapes.add(DrawableShape.Point(x, fx, radius = 6f, color = 0xFF00FF41.toInt()))

            frames.add(GeometryFrame(
                timestamp = i / (steps + 5.0),
                shapes = shapes,
                labels = listOf(
                    Label("割线 h=${"%.3f".format(hi)}", x + h, fx + dy * h + 0.3, 0xFFFF5252.toInt()),
                    Label("斜率≈${"%.4f".format(dy)}", x + h, fx + dy * h - 0.3, 0xFFFFA657.toInt())
                )
            ))
        }

        // ===== 阶段2：切线 + 法线 =====
        val tangentFrameShapes = mutableListOf<DrawableShape>()
        // 函数曲线（局部）
        val localCurve = (-20..20).map { j ->
            val xv = x + j * 0.1
            xv to f(xv)
        }
        tangentFrameShapes.add(DrawableShape.Path(localCurve, strokeWidth = 2f, color = 0xFF8B949E.toInt()))
        // 切线（绿色实线）
        tangentFrameShapes.add(DrawableShape.Line(
            x1 = x - 2.0, y1 = fx - slope * 2.0,
            x2 = x + 2.0, y2 = fx + slope * 2.0,
            strokeWidth = 2.5f, color = 0xFF00FF41.toInt()
        ))
        // 法线（冷灰虚线）
        val normalSlope = if (abs(slope) > 1e-6) -1.0 / slope else 0.0
        tangentFrameShapes.add(DrawableShape.Line(
            x1 = x - 1.5, y1 = fx - normalSlope * 1.5,
            x2 = x + 1.5, y2 = fx + normalSlope * 1.5,
            strokeWidth = 1.5f, color = 0xFF8B949E.toInt(), isDashed = true
        ))
        // 切点
        tangentFrameShapes.add(DrawableShape.Point(x, fx, radius = 6f, color = 0xFF00FF41.toInt()))

        frames.add(GeometryFrame(
            timestamp = (steps + 1) / (steps + 5.0),
            shapes = tangentFrameShapes,
            labels = listOf(
                Label("切线  f'(${"%.2f".format(x)}) = ${"%.4f".format(slope)}", x + 0.5, fx + slope * 0.5 + 0.5, 0xFF00FF41.toInt()),
                Label("法线", x + 0.5, fx + normalSlope * 0.5 - 0.5, 0xFF8B949E.toInt())
            )
        ))

        // ===== 阶段3：曲率圆 =====
        val curvatureShapes = mutableListOf<DrawableShape>()
        curvatureShapes.add(DrawableShape.Path(localCurve, strokeWidth = 2f, color = 0xFF8B949E.toInt()))
        curvatureShapes.add(DrawableShape.Line(
            x1 = x - 2.0, y1 = fx - slope * 2.0,
            x2 = x + 2.0, y2 = fx + slope * 2.0,
            strokeWidth = 1.5f, color = 0xFF00FF41.toInt()
        ))

        // 计算曲率半径和曲率中心
        val secondDerivative = (f(x + 0.01) - 2 * fx + f(x - 0.01)) / (0.01 * 0.01)
        val curvature = if (abs(secondDerivative) > 1e-8) {
            abs(secondDerivative) / (1 + slope * slope).pow(1.5)
        } else 0.0
        val radius = if (curvature > 1e-8) 1.0 / curvature else 1e6
        // 曲率中心位于法线方向上
        val normalDirX = if (abs(slope) > 1e-6) -slope else 0.0
        val normalDirY = 1.0
        val normalLen = sqrt(normalDirX * normalDirX + normalDirY * normalDirY)
        val centerX = x + normalDirX / normalLen * radius * if (secondDerivative > 0) 1 else -1
        val centerY = fx + normalDirY / normalLen * radius * if (secondDerivative > 0) 1 else -1

        if (radius < 100) {
            curvatureShapes.add(DrawableShape.Circle(
                cx = centerX, cy = centerY,
                radius = radius,
                fillColor = null,
                strokeColor = 0xFFFFA657.toInt(),
                strokeWidth = 2f
            ))
            // 曲率中心点
            curvatureShapes.add(DrawableShape.Point(centerX, centerY, radius = 3f, color = 0xFFFFA657.toInt()))
            // 半径线
            curvatureShapes.add(DrawableShape.Line(x, fx, centerX, centerY, color = 0xFFFFA657.toInt(), isDashed = true))
        }

        curvatureShapes.add(DrawableShape.Point(x, fx, radius = 6f, color = 0xFF00FF41.toInt()))

        frames.add(GeometryFrame(
            timestamp = (steps + 2) / (steps + 5.0),
            shapes = curvatureShapes,
            labels = listOf(
                Label("曲率半径 R = ${"%.3f".format(radius)}", x + 0.5, fx + 1.0, 0xFFFFA657.toInt()),
                Label("曲率 κ = ${"%.4f".format(curvature)}", x + 0.5, fx + 0.6, 0xFFFFA657.toInt())
            )
        ))

        // 空白过渡帧，让曲率圆停留一会儿
        repeat(3) { j ->
            frames.add(GeometryFrame(
                timestamp = (steps + 3 + j) / (steps + 5.0),
                shapes = curvatureShapes,
                labels = listOf(
                    Label("曲率半径 R = ${"%.3f".format(radius)}", x + 0.5, fx + 1.0, 0xFFFFA657.toInt()),
                    Label("曲率 κ = ${"%.4f".format(curvature)}", x + 0.5, fx + 0.6, 0xFFFFA657.toInt())
                )
            ))
        }

        val calcSteps = listOf(
            CalculusStep("前向差分", "f'($x) ≈ [f(${"%.4f".format(x + 0.01)}) - f($x)] / 0.01", "近似值: ${"%.6f".format(forward)}", forward),
            CalculusStep("后向差分", "f'($x) ≈ [f($x) - f(${"%.4f".format(x - 0.01)})] / 0.01", "近似值: ${"%.6f".format(backward)}", backward),
            CalculusStep("中心差分", "f'($x) ≈ [f(${"%.4f".format(x + 0.01)}) - f(${"%.4f".format(x - 0.01)})] / 0.02", "近似值: ${"%.6f".format(central)}", central)
        )

        return DerivativeResult(slope, fx, calcSteps, frames)
    }

    /**
     * 泰勒展开：生成逐阶逼近动画帧
     *
     * 改进：每帧保留所有低阶曲线，新阶曲线淡入叠加，
     * 形成多项式逐阶逼近原函数的视觉效果。
     */
    fun taylorExpansion(config: TaylorConfig): CalculusResult {
        val f = compileFunction(config.function)
        val a = config.center
        val n = config.order.coerceIn(1, 10)

        // 数值计算各阶导数
        val derivatives = mutableListOf<Double>()
        derivatives.add(f(a))
        var h = 0.01
        for (order in 1..n) {
            var d = f(a)
            repeat(order) { _ ->
                d = (f(a + h) - f(a - h)) / (2 * h)
                h *= 0.5
            }
            derivatives.add(d)
        }

        val steps = mutableListOf<CalculusStep>()
        val frames = mutableListOf<GeometryFrame>()

        steps.add(CalculusStep("展开中心", "a = $a", "在 x = $a 处展开"))

        val sampleX = (-5..50).map { a + (it - 25) * 0.2 }

        // 原始函数曲线点（始终显示，淡色）
        val originalPoints = sampleX.map { it to f(it) }
        val originalShape = DrawableShape.Path(originalPoints, strokeWidth = 2f, color = 0x448B949E.toInt())

        // 保留历史的多项式曲线
        val historicalPaths = mutableListOf<DrawableShape>()

        for (k in 1..n) {
            val shapes = mutableListOf<DrawableShape>()
            shapes.add(originalShape)

            // 低阶曲线（保留历史，越来越淡）
            for (prevK in 1 until k) {
                val prevPoints = sampleX.map { x ->
                    var sum = 0.0
                    for (i in 0..prevK) {
                        val coeff = derivatives.getOrElse(i) { 0.0 }
                        val fact = factorialInt(i)
                        sum += coeff / fact * (x - a).pow(i)
                    }
                    x to sum
                }
                val alpha = (0x20 + prevK * 0x08).coerceAtMost(0x40)
                val prevColor = 0xFF000000.toInt() or (alpha shl 24) or 0x00448AFF
                historicalPaths.add(DrawableShape.Path(prevPoints, strokeWidth = 1.5f, color = prevColor))
            }
            shapes.addAll(historicalPaths)

            // 当前 k 阶 Taylor 多项式（亮色）
            val taylorPoints = sampleX.map { x ->
                var sum = 0.0
                for (i in 0..k) {
                    val coeff = derivatives.getOrElse(i) { 0.0 }
                    val fact = factorialInt(i)
                    sum += coeff / fact * (x - a).pow(i)
                }
                x to sum
            }
            shapes.add(DrawableShape.Path(taylorPoints, strokeWidth = 3f, color = 0xFF448AFF.toInt()))

            // 展开中心标记
            shapes.add(DrawableShape.Circle(a, f(a), 0.12, fillColor = 0x2200FF41, strokeColor = 0xFF00FF41.toInt(), strokeWidth = 2f))

            frames.add(GeometryFrame(
                timestamp = k / n.toDouble(),
                shapes = shapes,
                labels = listOf(
                    Label("n=$k", a + 1.5, f(a) + 1.0, 0xFF448AFF.toInt()),
                    Label("原函数", a + 1.5, f(a) + 0.6, 0xFF8B949E.toInt())
                )
            ))

            val fact = factorialInt(k)
            steps.add(CalculusStep(
                title = "$k 阶展开",
                formula = "f^{($k)}($a) / $fact! · (x - $a)^$k",
                explanation = "${k}阶项系数: ${"%.4f".format(derivatives.getOrElse(k) { 0.0 })}",
                numericResult = derivatives.getOrElse(k) { 0.0 }
            ))
        }

        return CalculusResult(0.0, steps, frames)
    }

    /**
     * 极限可视化：ε-δ 带状区域动画帧
     */
    fun limitFrames(config: LimitConfig): List<GeometryFrame> {
        val f = compileFunction(config.function)
        val a = config.approachPoint
        val L = config.limitValue
        val epsilonRange = config.epsilonRange

        val frames = mutableListOf<GeometryFrame>()
        val steps = 30

        for (i in 1..steps) {
            val progress = i / steps.toDouble()
            val epsilon = epsilonRange * (1 - progress * 0.9)
            val delta = epsilon / 2.0

            val shapes = mutableListOf<DrawableShape>()

            // ε-带（水平）：[L-ε, L+ε]
            shapes.add(DrawableShape.Rect(
                x = a - 3.0, y = L - epsilon,
                width = 6.0, height = epsilon * 2,
                fillColor = 0x22448AFF, strokeColor = 0xFF448AFF.toInt()
            ))

            // δ-带（垂直）：[a-δ, a+δ]
            shapes.add(DrawableShape.Rect(
                x = a - delta, y = L - 2.0,
                width = delta * 2, height = 4.0,
                fillColor = 0x22FF5252, strokeColor = 0xFFFF5252.toInt()
            ))

            // 函数曲线
            val xs: List<Double> = (-20..20).map { idx -> a + idx * delta * 0.1 }
            val curvePoints: List<Pair<Double, Double>> = xs.map { xv -> xv to f(xv) }
            shapes.add(DrawableShape.Path(curvePoints, strokeWidth = 2f, color = 0xFF00FF41.toInt()))

            // 中心点与极限点
            shapes.add(DrawableShape.Point(a, L, radius = 6f, color = 0xFF00FF41.toInt()))
            shapes.add(DrawableShape.Line(a, L - epsilon, a, L + epsilon, isDashed = true, color = 0xFF448AFF.toInt()))
            shapes.add(DrawableShape.Line(a - delta, L, a + delta, L, isDashed = true, color = 0xFFFF5252.toInt()))

            frames.add(GeometryFrame(
                timestamp = progress,
                shapes = shapes,
                labels = listOf(
                    Label("ε=${"%.3f".format(epsilon)}", a + 1.5, L + epsilon + 0.3, 0xFF448AFF.toInt()),
                    Label("δ=${"%.3f".format(delta)}", a + delta + 0.2, L - 1.5, 0xFFFF5252.toInt()),
                    Label("lim x→$a = $L", a - 2.5, L + 1.5)
                )
            ))
        }

        return frames
    }

    // ==================== 工具方法 ====================

    private fun factorialInt(n: Int): Long {
        var result = 1L
        for (i in 2..n) result *= i
        return result
    }

    /**
     * 简单解析积分（用于计算真实值对比）
     * 仅支持多项式形式，其他函数返回数值近似
     */
    private fun trueIntegral(expr: String, a: Double, b: Double): Double {
        return try {
            // 数值近似：Simpson with 1000 subdivisions
            val f = compileFunction(expr)
            val n = 1000
            val dx = (b - a) / n
            var sum = f(a) + f(b)
            for (i in 1 until n) {
                val x = a + i * dx
                sum += if (i % 2 == 0) 2 * f(x) else 4 * f(x)
            }
            sum * dx / 3
        } catch (_: Exception) {
            0.0
        }
    }

    private fun compileFunction(expr: String): (Double) -> Double {
        return { x ->
            try {
                val substituted = expr.replace("x", x.toString())
                    .replace("theta", x.toString())
                    .replace("t", x.toString())
                val lexer = Lexer(substituted)
                val tokens = lexer.tokenize()
                val parser = Parser(tokens)
                val ast = parser.parse()
                if (ast != null) Evaluator.evaluate(ast) else 0.0
            } catch (_: Exception) {
                0.0
            }
        }
    }
}
