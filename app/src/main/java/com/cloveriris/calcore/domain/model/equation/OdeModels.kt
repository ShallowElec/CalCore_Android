package com.cloveriris.calcore.domain.model.equation

/**
 * 常微分方程 (ODE) 与偏微分方程 (PDE) 的数据模型
 */

/**
 * ODE 定义
 */
data class OdeDefinition(
    val type: OdeType,
    val equation: String,              // 如 "dy/dt = -lambda * y"
    val variables: List<String>,       // 状态变量，如 ["y"]
    val parameters: Map<String, Double>,
    val initialConditions: Map<String, Double>,
    val timeRange: ClosedFloatingPointRange<Double>,
    val solverConfig: SolverConfig = SolverConfig()
)

enum class OdeType {
    FIRST_ORDER_SCALAR,        // dy/dt = f(t,y)
    FIRST_ORDER_SYSTEM,        // dy/dt = f(t, y_vector)
    SECOND_ORDER_SCALAR        // d²y/dt² = f(t, y, dy/dt)
}

/**
 * PDE 定义（目前支持一维时空演化）
 */
data class PdeDefinition(
    val type: PdeType,
    val equation: String,
    val spatialVariable: String = "x",
    val timeVariable: String = "t",
    val parameters: Map<String, Double>,
    val spatialRange: ClosedFloatingPointRange<Double>,
    val timeRange: ClosedFloatingPointRange<Double>,
    val boundaryConditions: List<BoundaryCondition>,
    val initialCondition: String,      // 初值函数，如 "sin(pi*x/L)"
    val solverConfig: SolverConfig = SolverConfig()
)

enum class PdeType {
    HEAT_1D,      // ∂u/∂t = α·∂²u/∂x²
    WAVE_1D       // ∂²u/∂t² = c²·∂²u/∂x²
}

data class BoundaryCondition(
    val location: BoundaryLocation,
    val type: BoundaryType,
    val value: Double = 0.0
)

enum class BoundaryLocation { LEFT, RIGHT }
enum class BoundaryType { DIRICHLET, NEUMANN }

/**
 * 数值求解器配置
 */
data class SolverConfig(
    val method: SolverMethod = SolverMethod.RK4,
    val timeStep: Double = 0.01,
    val spatialStep: Double = 0.1,
    val adaptive: Boolean = false
)

enum class SolverMethod {
    EULER,
    EULER_IMPROVED,
    RK4,
    RKF45         // Runge-Kutta-Fehlberg (adaptive)
}

/**
 * ODE 求解结果
 */
data class OdeSolution(
    val timePoints: List<Double>,
    val values: List<List<Double>>,    // [timeIndex][variableIndex]
    val variables: List<String>
)

/**
 * PDE 求解结果（时间切片序列）
 */
data class PdeSolution(
    val timePoints: List<Double>,
    val spacePoints: List<Double>,
    val slices: List<TimeSlice>        // 每个时间步一个空间场
)

data class TimeSlice(
    val time: Double,
    val values: List<Double>           // 对应 spacePoints 的场值
)
