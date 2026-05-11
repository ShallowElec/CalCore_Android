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
     * 将字符串表达式编译为可调用函数
     * TODO: 未来支持变量替换（不只是 x）
     */
    private fun compileFunction(expr: String): (Double) -> Double {
        return { x ->
            try {
                // 简单替换 x 为数值，然后用现有 parser 求值
                val substituted = expr.replace("x", x.toString())
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
