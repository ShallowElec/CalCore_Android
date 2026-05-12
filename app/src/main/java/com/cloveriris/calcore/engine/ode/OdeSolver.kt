package com.cloveriris.calcore.engine.ode

import com.cloveriris.calcore.domain.model.equation.OdeDefinition
import com.cloveriris.calcore.domain.model.equation.OdeSolution
import com.cloveriris.calcore.domain.model.equation.SolverMethod
import kotlin.math.pow

/**
 * ODE 数值求解引擎
 *
 * 支持 Euler、改进 Euler (Heun)、RK4、RKF45 自适应步长。
 * 纯算法层，通过函数引用支持任意 ODE 系统 dy/dt = f(t, y)。
 */
object OdeSolver {

    /**
     * 通用求解入口
     *
     * @param ode ODE 定义
     * @param dydt 微分方程右侧函数: (t, y_vector) -> dy/dt_vector
     */
    fun solve(ode: OdeDefinition, dydt: (Double, List<Double>) -> List<Double>): OdeSolution {
        return when (ode.solverConfig.method) {
            SolverMethod.EULER -> solveEuler(ode, dydt)
            SolverMethod.EULER_IMPROVED -> solveHeun(ode, dydt)
            SolverMethod.RK4 -> solveRK4(ode, dydt)
            SolverMethod.RKF45 -> solveRKF45(ode, dydt)
        }
    }

    // 兼容旧接口：硬编码指数衰减（单变量）
    fun solve(ode: OdeDefinition): OdeSolution {
        val lambda = ode.parameters["lambda"] ?: 0.5
        return solve(ode) { _, y ->
            listOf(-lambda * y[0])
        }
    }

    private fun solveEuler(
        ode: OdeDefinition,
        dydt: (Double, List<Double>) -> List<Double>
    ): OdeSolution {
        val dt = ode.solverConfig.timeStep
        val t0 = ode.timeRange.start
        val tEnd = ode.timeRange.endInclusive
        val y0 = ode.initialConditions.values.toList()
        val n = ode.initialConditions.size

        val times = mutableListOf<Double>()
        val values = mutableListOf<List<Double>>()

        var t = t0
        var y = y0.toMutableList()
        while (t <= tEnd) {
            times.add(t)
            values.add(y.toList())
            val slopes = dydt(t, y)
            for (i in 0 until n) {
                y[i] += dt * slopes[i]
            }
            t += dt
        }

        return OdeSolution(times, values, ode.variables)
    }

    private fun solveHeun(
        ode: OdeDefinition,
        dydt: (Double, List<Double>) -> List<Double>
    ): OdeSolution {
        val dt = ode.solverConfig.timeStep
        val t0 = ode.timeRange.start
        val tEnd = ode.timeRange.endInclusive
        val y0 = ode.initialConditions.values.toList()
        val n = ode.initialConditions.size

        val times = mutableListOf<Double>()
        val values = mutableListOf<List<Double>>()

        var t = t0
        var y = y0.toMutableList()
        while (t <= tEnd) {
            times.add(t)
            values.add(y.toList())

            val k1 = dydt(t, y)
            val yPred = MutableList(n) { i -> y[i] + dt * k1[i] }
            val k2 = dydt(t + dt, yPred)

            for (i in 0 until n) {
                y[i] += dt / 2 * (k1[i] + k2[i])
            }
            t += dt
        }

        return OdeSolution(times, values, ode.variables)
    }

    private fun solveRK4(
        ode: OdeDefinition,
        dydt: (Double, List<Double>) -> List<Double>
    ): OdeSolution {
        val dt = ode.solverConfig.timeStep
        val t0 = ode.timeRange.start
        val tEnd = ode.timeRange.endInclusive
        val y0 = ode.initialConditions.values.toList()
        val n = ode.initialConditions.size

        val times = mutableListOf<Double>()
        val values = mutableListOf<List<Double>>()

        var t = t0
        var y = y0.toMutableList()
        while (t <= tEnd) {
            times.add(t)
            values.add(y.toList())

            val k1 = dydt(t, y)
            val y2 = MutableList(n) { i -> y[i] + dt / 2 * k1[i] }
            val k2 = dydt(t + dt / 2, y2)
            val y3 = MutableList(n) { i -> y[i] + dt / 2 * k2[i] }
            val k3 = dydt(t + dt / 2, y3)
            val y4 = MutableList(n) { i -> y[i] + dt * k3[i] }
            val k4 = dydt(t + dt, y4)

            for (i in 0 until n) {
                y[i] += dt / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i])
            }
            t += dt
        }

        return OdeSolution(times, values, ode.variables)
    }

    /**
     * RKF45 (Runge-Kutta-Fehlberg) 自适应步长
     *
     * 使用 4 阶和 5 阶公式的局部截断误差估计来自动调整步长。
     */
    private fun solveRKF45(
        ode: OdeDefinition,
        dydt: (Double, List<Double>) -> List<Double>
    ): OdeSolution {
        val dtInitial = ode.solverConfig.timeStep
        val t0 = ode.timeRange.start
        val tEnd = ode.timeRange.endInclusive
        val y0 = ode.initialConditions.values.toList()
        val n = ode.initialConditions.size
        val tolerance = 1e-6
        val safety = 0.9

        val times = mutableListOf<Double>()
        val values = mutableListOf<List<Double>>()

        var t = t0
        var y = y0.toMutableList()
        var dt = dtInitial

        while (t <= tEnd) {
            times.add(t)
            values.add(y.toList())

            if (t + dt > tEnd) dt = tEnd - t

            val k1 = dydt(t, y)
            val y2 = MutableList(n) { i -> y[i] + dt / 4 * k1[i] }
            val k2 = dydt(t + dt / 4, y2)
            val y3 = MutableList(n) { i -> y[i] + dt * (3 * k1[i] + 9 * k2[i]) / 32 }
            val k3 = dydt(t + 3 * dt / 8, y3)
            val y4 = MutableList(n) { i ->
                y[i] + dt * (1932 * k1[i] - 7200 * k2[i] + 7296 * k3[i]) / 2197
            }
            val k4 = dydt(t + 12 * dt / 13, y4)
            val y5 = MutableList(n) { i ->
                y[i] + dt * (439 * k1[i] / 216 - 8 * k2[i] + 3680 * k3[i] / 513 - 845 * k4[i] / 4104)
            }
            val k5 = dydt(t + dt, y5)
            val y6 = MutableList(n) { i ->
                y[i] + dt * (-8 * k1[i] / 27 + 2 * k2[i] - 3544 * k3[i] / 2565 + 1859 * k4[i] / 4104 - 11 * k5[i] / 40)
            }
            val k6 = dydt(t + dt / 2, y6)

            // 4 阶和 5 阶解
            val y4th = MutableList(n) { i ->
                y[i] + dt * (25 * k1[i] / 216 + 1408 * k3[i] / 2565 + 2197 * k4[i] / 4104 - k5[i] / 5)
            }
            val y5th = MutableList(n) { i ->
                y[i] + dt * (16 * k1[i] / 135 + 6656 * k3[i] / 12825 + 28561 * k4[i] / 56430 - 9 * k5[i] / 50 + 2 * k6[i] / 55)
            }

            val error = (0 until n).maxOf { i -> kotlin.math.abs(y5th[i] - y4th[i]) }

            if (error < tolerance || dt < 1e-8) {
                // 接受这一步
                for (i in 0 until n) y[i] = y4th[i]
                t += dt
            }

            // 调整步长
            dt = if (error > 0) {
                dt * safety * (tolerance / error).pow(0.25)
            } else {
                dt * 2
            }.coerceIn(1e-8, dtInitial * 10)
        }

        return OdeSolution(times, values, ode.variables)
    }
}
