package com.cloveriris.calcore.engine.graphing

import com.cloveriris.calcore.domain.model.graphing.GraphPoint

/**
 * 显函数等距采样器（纯算法，无 Android 依赖）
 */
object FunctionSampler {

    /**
     * 对显函数 y = f(x) 进行等距采样
     *
     * @param evaluate 求值回调，返回 Double 或失败
     * @param expression 表达式字符串（如 "x^2 + a"）
     * @param variables 固定变量值（参数，如 {"a": 1.0}）
     * @param xRange x 采样范围
     * @param sampleCount 采样点数，默认 800
     * @return 世界坐标点列表（跳过求值失败的点）
     */
    fun sampleExplicitY(
        evaluate: (expr: String, vars: Map<String, Double>) -> Result<Double>,
        expression: String,
        variables: Map<String, Double>,
        xRange: ClosedFloatingPointRange<Double>,
        sampleCount: Int = 800
    ): List<GraphPoint> {
        if (sampleCount <= 0) return emptyList()

        val step = if (sampleCount > 1) {
            (xRange.endInclusive - xRange.start) / (sampleCount - 1)
        } else {
            0.0
        }

        return buildList {
            for (i in 0 until sampleCount) {
                val x = xRange.start + i * step
                val vars = variables + ("x" to x)
                val result = evaluate(expression, vars)
                result.onSuccess { y ->
                    add(GraphPoint(x, y))
                }
                // 失败则跳过该点，保持折线连续性
            }
        }
    }
}
