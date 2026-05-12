package com.cloveriris.calcore.presentation.diffeq

import com.cloveriris.calcore.domain.model.equation.OdeSolution
import com.cloveriris.calcore.domain.model.equation.SolverMethod

/**
 * 微分方程工作台 UI 状态
 */
data class DiffEqUiState(
    val selectedTemplateIndex: Int = 0,
    val solverMethod: SolverMethod = SolverMethod.RK4,
    val timeStep: Double = 0.01,
    val timeEnd: Double = 10.0,
    val parameters: Map<String, Double> = emptyMap(),
    val solution: OdeSolution? = null,
    val isPlaying: Boolean = false,
    val currentTimeIndex: Int = 0,
    val showDirectionField: Boolean = true,
    val showPhasePortrait: Boolean = false,
    val viewport: DiffEqViewport = DiffEqViewport()
)

/**
 * ODE 模板定义
 */
data class OdeTemplate(
    val id: String,
    val name: String,
    val description: String,
    val equation: String,
    val variableCount: Int,
    val variableNames: List<String>,
    val defaultParameters: Map<String, Double>,
    val defaultInitial: Map<String, Double>,
    val defaultTimeEnd: Double,
    val dydt: (Double, List<Double>, Map<String, Double>) -> List<Double>
)

/**
 * 画布视口
 */
data class DiffEqViewport(
    val minX: Double = -1.0,
    val maxX: Double = 11.0,
    val minY: Double = -2.0,
    val maxY: Double = 2.0
)
