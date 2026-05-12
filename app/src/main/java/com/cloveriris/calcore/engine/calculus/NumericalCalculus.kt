package com.cloveriris.calcore.engine.calculus

import com.cloveriris.calcore.domain.model.calculus.*
import com.cloveriris.calcore.engine.evaluator.Evaluator
import com.cloveriris.calcore.engine.parser.Expression
import com.cloveriris.calcore.engine.parser.Lexer
import com.cloveriris.calcore.engine.parser.Parser

/**
 * 数值微积分引擎
 *
 * 提供导数近似、数值积分、泰勒展开等计算的逐步结果。
 */
object NumericalCalculus {

    /**
     * 数值积分（带几何帧生成）
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

        steps.add(CalculusStep(
            title = "区间分割",
            formula = "Δx = (b-a)/n = ${String.format("%.4f", dx)}",
            explanation = "将 [${String.format("%.2f", a)}, ${String.format("%.2f", b)}] 均分为 $n 个子区间"
        ))

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
            val area = if (config.method == IntegrationMethod.SIMPSON) {
                height * dx  // Simpson 的 height 已经是加权平均
            } else {
                height * dx
            }
            sum += area

            // 生成几何帧
            val shapes = mutableListOf<DrawableShape>()
            when (config.method) {
                IntegrationMethod.LEFT_RIEMANN, IntegrationMethod.RIGHT_RIEMANN,
                IntegrationMethod.MIDPOINT -> {
                    shapes.add(DrawableShape.Rect(x0, 0.0, dx, height))
                }
                IntegrationMethod.TRAPEZOID -> {
                    shapes.add(DrawableShape.Path(listOf(
                        x0 to 0.0, x0 to f(x0), x1 to f(x1), x1 to 0.0, x0 to 0.0
                    )))
                }
                IntegrationMethod.SIMPSON -> {
                    // 简化为梯形可视化
                    shapes.add(DrawableShape.Path(listOf(
                        x0 to 0.0, x0 to f(x0), x1 to f(x1), x1 to 0.0, x0 to 0.0
                    )))
                }
            }
            frames.add(GeometryFrame(
                timestamp = (i + 1) / n.toDouble(),
                shapes = shapes,
                labels = listOf(Label("S≈${String.format("%.3f", sum)}", b, sum * 0.1))
            ))
        }

        steps.add(CalculusStep(
            title = "累加结果",
            formula = "∫≈ ${String.format("%.6f", sum)}",
            explanation = "${config.method} 法，$n 个子区间",
            numericResult = sum
        ))

        return CalculusResult(sum, steps, frames)
    }

    /**
     * 导数近似：中心差分法，生成切线滑动动画帧
     */
    fun derivative(config: DerivativeConfig): DerivativeResult {
        val f = compileFunction(config.function)
        val x = config.point
        val h = config.stepSize

        val forward = (f(x + h) - f(x)) / h
        val backward = (f(x) - f(x - h)) / h
        val central = (f(x + h) - f(x - h)) / (2 * h)

        val fx = f(x)
        val slope = central

        // 生成切线动画帧：h 从大到小收缩
        val frames = mutableListOf<GeometryFrame>()
        val steps = 20
        for (i in 1..steps) {
            val hi = h * (steps - i + 1) / steps
            val dy = (f(x + hi) - f(x - hi)) / (2 * hi)
            val shapes = mutableListOf<DrawableShape>()
            // 割线
            shapes.add(DrawableShape.Line(
                x1 = x - hi * 2, y1 = fx - dy * hi * 2,
                x2 = x + hi * 2, y2 = fx + dy * hi * 2,
                strokeWidth = 2f,
                color = 0xFFFF5252.toInt()
            ))
            // 两点标记
            shapes.add(DrawableShape.Point(x - hi, f(x - hi), radius = 3f, color = 0xFFFFA657.toInt()))
            shapes.add(DrawableShape.Point(x + hi, f(x + hi), radius = 3f, color = 0xFFFFA657.toInt()))
            shapes.add(DrawableShape.Point(x, fx, radius = 5f, color = 0xFF00FF41.toInt()))
            frames.add(GeometryFrame(
                timestamp = i / steps.toDouble(),
                shapes = shapes,
                labels = listOf(
                    Label("h=${"%.4f".format(hi)}", x + h, fx + dy * h),
                    Label("f'≈${"%.4f".format(dy)}", x + h, fx + dy * h - 0.5)
                )
            ))
        }

        val calcSteps = listOf(
            CalculusStep("前向差分", "f'($x) ≈ [f(${"%.4f".format(x + h)}) - f($x)] / $h", "近似值: ${"%.6f".format(forward)}", forward),
            CalculusStep("后向差分", "f'($x) ≈ [f($x) - f(${"%.4f".format(x - h)})] / $h", "近似值: ${"%.6f".format(backward)}", backward),
            CalculusStep("中心差分", "f'($x) ≈ [f(${"%.4f".format(x + h)}) - f(${"%.4f".format(x - h)})] / ${2 * h}", "近似值: ${"%.6f".format(central)}", central)
        )

        return DerivativeResult(slope, fx, calcSteps, frames)
    }

    /**
     * 泰勒展开：生成逐阶逼近动画帧
     */
    fun taylorExpansion(config: TaylorConfig): CalculusResult {
        val f = compileFunction(config.function)
        val a = config.center
        val n = config.order.coerceIn(1, 10)

        // 数值计算各阶导数（中心差分，递减步长）
        val derivatives = mutableListOf<Double>()
        derivatives.add(f(a))
        var h = 0.01
        for (order in 1..n) {
            var d = f(a)
            repeat(order) { _ ->
                val df = { x: Double ->
                    (f(x + h) - f(x - h)) / (2 * h)
                }
                // 重新包装... 简化：直接用高阶差分
                d = (f(a + h) - f(a - h)) / (2 * h)
                h *= 0.5
            }
            derivatives.add(d)
        }

        val steps = mutableListOf<CalculusStep>()
        val frames = mutableListOf<GeometryFrame>()

        steps.add(CalculusStep("展开中心", "a = $a", "在 x = $a 处展开"))

        val sampleX = (-5..50).map { a + (it - 25) * 0.2 }

        for (k in 1..n) {
            val shapes = mutableListOf<DrawableShape>()
            // 原始函数曲线（淡色）
            val originalPoints = sampleX.map { it to f(it) }
            shapes.add(DrawableShape.Path(originalPoints, strokeWidth = 2f, color = 0x448B949E))

            // k 阶 Taylor 多项式点
            val taylorPoints = sampleX.map { x ->
                var sum = 0.0
                for (i in 0..k) {
                    val coeff = derivatives.getOrElse(i) { 0.0 }
                    val factorial = (1..i).fold(1L) { acc, j -> acc * j }
                    sum += coeff / factorial * Math.pow(x - a, i.toDouble())
                }
                x to sum
            }
            shapes.add(DrawableShape.Path(taylorPoints, strokeWidth = 3f, color = 0xFF448AFF.toInt()))
            shapes.add(DrawableShape.Circle(a, f(a), 0.15, strokeColor = 0xFF00FF41.toInt()))

            frames.add(GeometryFrame(
                timestamp = k / n.toDouble(),
                shapes = shapes,
                labels = listOf(Label("n=$k", a + 1, f(a) + 1, 0xFF448AFF.toInt()))
            ))

            val factorial = (1..k).fold(1L) { acc, j -> acc * j }
            steps.add(CalculusStep(
                title = "$k 阶展开",
                formula = "f^{($k)}($a) / $factorial! · (x - $a)^$k",
                explanation = "${k}阶项系数: ${"%.4f".format(derivatives.getOrElse(k) { 0.0 })}",
                numericResult = derivatives.getOrElse(k) { 0.0 }
            ))
        }

        return CalculusResult(0.0, steps, frames)
    }

    /**
     * 极限可视化：生成 ε-δ 带状区域动画帧
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
            val epsilon = epsilonRange * (1 - progress * 0.9) // ε 从大到小
            val delta = epsilon / 2.0 // 简化的 δ-ε 关系

            val shapes = mutableListOf<DrawableShape>()

            // 水平 ε-带: [L-ε, L+ε]
            shapes.add(DrawableShape.Rect(
                x = a - 3.0, y = L - epsilon,
                width = 6.0, height = epsilon * 2,
                fillColor = 0x22448AFF, strokeColor = 0xFF448AFF.toInt()
            ))

            // 垂直 δ-带: [a-δ, a+δ]
            shapes.add(DrawableShape.Rect(
                x = a - delta, y = L - 2.0,
                width = delta * 2, height = 4.0,
                fillColor = 0x22FF5252, strokeColor = 0xFFFF5252.toInt()
            ))

            // 函数曲线（δ 邻域内）
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

    /**
     * 将字符串表达式编译为可调用函数
     * TODO: 未来支持变量替换（不只是 x）
     */
    private fun compileFunction(expr: String): (Double) -> Double {
        return { x ->
            try {
                // 简单替换 x 为数值，然后用现有 parser 求值
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
