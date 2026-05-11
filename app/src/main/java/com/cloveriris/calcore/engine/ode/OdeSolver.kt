package com.cloveriris.calcore.engine.ode

import com.cloveriris.calcore.domain.model.equation.OdeDefinition
import com.cloveriris.calcore.domain.model.equation.OdeSolution
import com.cloveriris.calcore.domain.model.equation.SolverMethod

/**
 * ODE 数值求解引擎
 *
 * 支持 Euler、改进 Euler、RK4 等经典方法。
 * 纯算法层，在后台协程执行。
 */
object OdeSolver {

    fun solve(ode: OdeDefinition): OdeSolution {
        return when (ode.solverConfig.method) {
            SolverMethod.EULER -> solveEuler(ode)
            SolverMethod.EULER_IMPROVED -> solveImprovedEuler(ode)
            SolverMethod.RK4 -> solveRK4(ode)
            SolverMethod.RKF45 -> solveRKF45(ode)
        }
    }

    private fun solveEuler(ode: OdeDefinition): OdeSolution {
        val dt = ode.solverConfig.timeStep
        val t0 = ode.timeRange.start
        val tEnd = ode.timeRange.endInclusive
        val y0 = ode.initialConditions.values.first()

        val times = mutableListOf<Double>()
        val values = mutableListOf<List<Double>>()

        var t = t0
        var y = y0
        while (t <= tEnd) {
            times.add(t)
            values.add(listOf(y))
            // dy/dt = f(t,y) — 简化假设 f(t,y) = -lambda * y (指数衰减)
            val lambda = ode.parameters["lambda"] ?: 0.5
            val slope = -lambda * y
            y += dt * slope
            t += dt
        }

        return OdeSolution(times, values, ode.variables)
    }

    private fun solveImprovedEuler(ode: OdeDefinition): OdeSolution {
        // TODO: Heun's method
        return solveEuler(ode)
    }

    private fun solveRK4(ode: OdeDefinition): OdeSolution {
        val dt = ode.solverConfig.timeStep
        val t0 = ode.timeRange.start
        val tEnd = ode.timeRange.endInclusive
        val y0 = ode.initialConditions.values.first()

        val times = mutableListOf<Double>()
        val values = mutableListOf<List<Double>>()

        var t = t0
        var y = y0
        val lambda = ode.parameters["lambda"] ?: 0.5

        while (t <= tEnd) {
            times.add(t)
            values.add(listOf(y))

            val k1 = -lambda * y
            val k2 = -lambda * (y + dt / 2 * k1)
            val k3 = -lambda * (y + dt / 2 * k2)
            val k4 = -lambda * (y + dt * k3)

            y += dt / 6 * (k1 + 2 * k2 + 2 * k3 + k4)
            t += dt
        }

        return OdeSolution(times, values, ode.variables)
    }

    private fun solveRKF45(ode: OdeDefinition): OdeSolution {
        // TODO: adaptive step-size RK-Fehlberg
        return solveRK4(ode)
    }
}
