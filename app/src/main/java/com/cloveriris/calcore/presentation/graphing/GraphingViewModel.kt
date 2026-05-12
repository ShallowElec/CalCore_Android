package com.cloveriris.calcore.presentation.graphing

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.graphing.ExpressionType
import com.cloveriris.calcore.domain.model.graphing.GraphExpression
import com.cloveriris.calcore.domain.model.graphing.GraphPoint
import com.cloveriris.calcore.domain.model.graphing.GraphShape
import com.cloveriris.calcore.domain.model.graphing.ViewportState
import com.cloveriris.calcore.domain.usecase.EvaluateUseCase
import com.cloveriris.calcore.engine.evaluator.Evaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@HiltViewModel
class GraphingViewModel @Inject constructor(
    private val evaluateUseCase: EvaluateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphingUiState())
    val uiState: StateFlow<GraphingUiState> = _uiState.asStateFlow()

    private var sampleJob: Job? = null
    private var canvasWidth: Float = 800f
    private var canvasHeight: Float = 600f

    init {
        // 预置 5 条默认表达式，覆盖多种类型
        val presets = listOf(
            GraphExpression(rawExpression = "x^2", color = Color(0xFFFF5252)),
            GraphExpression(rawExpression = "sin(x)", color = Color(0xFF69F0AE)),
            GraphExpression(rawExpression = "x^3 - 2*x", color = Color(0xFF448AFF)),
            GraphExpression(rawExpression = "sqrt(abs(x))", color = Color(0xFFFFD740)),
            GraphExpression(rawExpression = "1/x", color = Color(0xFFE040FB)),
            // 极坐标示例：心形线 r = 1 + sin(θ)
            GraphExpression(
                rawExpression = "1 + sin(theta)",
                displayExpression = "r = 1 + sin(θ)",
                color = Color(0xFF18FFFF),
                type = ExpressionType.POLAR
            ),
            // 参数曲线示例：圆 (cos(t), sin(t))
            GraphExpression(
                rawExpression = "cos(t)",
                rawExpressionSecondary = "sin(t)",
                displayExpression = "(cos(t), sin(t))",
                color = Color(0xFFFF6E40),
                type = ExpressionType.PARAMETRIC
            ),
        )
        _uiState.update { it.copy(expressions = presets) }
        resample()
    }

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
        resample()
    }

    fun addExpression(raw: String, color: Color, type: ExpressionType = ExpressionType.EXPLICIT_Y) {
        val (primary, secondary, display) = when (type) {
            ExpressionType.PARAMETRIC -> {
                // 支持格式 "x(t), y(t)" 或 "x(t);y(t)"
                val cleaned = raw.replace(" ", "").replace("(", "").replace(")", "")
                val parts = cleaned.split(",", ";")
                if (parts.size >= 2) {
                    Triple(parts[0], parts[1], "(${parts[0]}, ${parts[1]})")
                } else {
                    Triple(raw, null, raw)
                }
            }
            ExpressionType.POLAR -> {
                Triple(raw, null, "r = $raw")
            }
            else -> Triple(raw, null, raw)
        }
        _uiState.update { current ->
            val expr = GraphExpression(
                rawExpression = primary,
                rawExpressionSecondary = secondary,
                displayExpression = display,
                color = color,
                type = type
            )
            current.copy(expressions = current.expressions + expr)
        }
        resample()
    }

    fun removeExpression(id: String) {
        _uiState.update { current ->
            current.copy(
                expressions = current.expressions.filter { it.id != id },
                selectedExpressionId = if (current.selectedExpressionId == id) null else current.selectedExpressionId
            )
        }
        resample()
    }

    fun toggleExpressionVisibility(id: String) {
        _uiState.update { current ->
            current.copy(
                expressions = current.expressions.map {
                    if (it.id == id) it.copy(isVisible = !it.isVisible) else it
                }
            )
        }
        resample()
    }

    fun selectExpression(id: String?) {
        _uiState.update { it.copy(selectedExpressionId = id) }
    }

    fun addParameter(name: String, default: Double) {
        _uiState.update { current ->
            if (name in current.parameters) return@update current
            current.copy(parameters = current.parameters + (name to default))
        }
        resample()
    }

    fun removeParameter(name: String) {
        _uiState.update { current ->
            current.copy(parameters = current.parameters - name)
        }
        resample()
    }

    fun updateParameter(name: String, value: Double) {
        _uiState.update { current ->
            if (!current.parameters.containsKey(name)) return@update current
            current.copy(parameters = current.parameters + (name to value))
        }
        resample()
    }

    fun updateViewport(viewport: ViewportState) {
        val oldViewport = _uiState.value.viewport
        _uiState.update { it.copy(viewport = viewport) }
        val scaleChanged = kotlin.math.abs(oldViewport.scale - viewport.scale) > 0.1
        val dx = kotlin.math.abs(oldViewport.centerX - viewport.centerX)
        val dy = kotlin.math.abs(oldViewport.centerY - viewport.centerY)
        val worldWidth = canvasWidth / viewport.scale
        val worldHeight = canvasHeight / viewport.scale
        val centerMoved = dx > worldWidth / 4 || dy > worldHeight / 4
        if (scaleChanged || centerMoved) {
            resample()
        }
    }

    fun resetViewport() {
        _uiState.update { it.copy(viewport = ViewportState()) }
        resample()
    }

    private fun resample() {
        sampleJob?.cancel()
        sampleJob = viewModelScope.launch(Dispatchers.Default) {
            val current = _uiState.value
            val viewport = current.viewport
            val worldWidth = canvasWidth / viewport.scale
            val worldHeight = canvasHeight / viewport.scale
            val xMin = viewport.centerX - worldWidth
            val xMax = viewport.centerX + worldWidth
            val sampleCount = (canvasWidth * 2).toInt().coerceIn(200, 2000)

            val shapes = current.expressions
                .filter { it.isVisible }
                .flatMap { expr ->
                    sampleExpression(expr, current.parameters, xMin..xMax, sampleCount)
                }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(shapes = shapes) }
            }
        }
    }

    private fun sampleExpression(
        expression: GraphExpression,
        parameters: Map<String, Double>,
        xRange: ClosedFloatingPointRange<Double>,
        sampleCount: Int
    ): List<GraphShape> {
        return when (expression.type) {
            ExpressionType.EXPLICIT_Y -> sampleExplicitY(expression, parameters, xRange, sampleCount)
            ExpressionType.POLAR -> samplePolar(expression, parameters, sampleCount)
            ExpressionType.PARAMETRIC -> sampleParametric(expression, parameters, sampleCount)
            else -> emptyList()
        }
    }

    private fun sampleExplicitY(
        expression: GraphExpression,
        parameters: Map<String, Double>,
        xRange: ClosedFloatingPointRange<Double>,
        sampleCount: Int
    ): List<GraphShape> {
        val ast = evaluateUseCase.parse(expression.rawExpression).getOrNull()
            ?: return emptyList()
        val points = mutableListOf<GraphPoint>()
        val step = if (sampleCount > 1) (xRange.endInclusive - xRange.start) / (sampleCount - 1) else 0.0

        for (i in 0 until sampleCount) {
            val x = xRange.start + i * step
            val y = try {
                Evaluator.evaluate(ast, parameters + ("x" to x))
            } catch (_: Exception) { continue }
            if (y.isFinite()) points.add(GraphPoint(x, y))
        }
        return if (points.size >= 2) listOf(GraphShape.Polyline(points, expression.color)) else emptyList()
    }

    private fun samplePolar(
        expression: GraphExpression,
        parameters: Map<String, Double>,
        sampleCount: Int
    ): List<GraphShape> {
        val ast = evaluateUseCase.parse(expression.rawExpression).getOrNull()
            ?: return emptyList()
        val points = mutableListOf<GraphPoint>()
        val thetaMin = 0.0
        val thetaMax = 2 * kotlin.math.PI
        val step = if (sampleCount > 1) (thetaMax - thetaMin) / (sampleCount - 1) else 0.0

        for (i in 0 until sampleCount) {
            val theta = thetaMin + i * step
            val r = try {
                Evaluator.evaluate(ast, parameters + ("theta" to theta))
            } catch (_: Exception) { continue }
            if (r.isFinite()) {
                val x = r * cos(theta)
                val y = r * sin(theta)
                points.add(GraphPoint(x, y))
            }
        }
        return if (points.size >= 2) listOf(GraphShape.Polyline(points, expression.color)) else emptyList()
    }

    private fun sampleParametric(
        expression: GraphExpression,
        parameters: Map<String, Double>,
        sampleCount: Int
    ): List<GraphShape> {
        val astX = evaluateUseCase.parse(expression.rawExpression).getOrNull()
            ?: return emptyList()
        val astY = expression.rawExpressionSecondary?.let {
            evaluateUseCase.parse(it).getOrNull()
        } ?: return emptyList()
        val points = mutableListOf<GraphPoint>()
        val tMin = 0.0
        val tMax = 2 * kotlin.math.PI
        val step = if (sampleCount > 1) (tMax - tMin) / (sampleCount - 1) else 0.0

        for (i in 0 until sampleCount) {
            val t = tMin + i * step
            val x = try {
                Evaluator.evaluate(astX, parameters + ("t" to t))
            } catch (_: Exception) { continue }
            val y = try {
                Evaluator.evaluate(astY, parameters + ("t" to t))
            } catch (_: Exception) { continue }
            if (x.isFinite() && y.isFinite()) {
                points.add(GraphPoint(x, y))
            }
        }
        return if (points.size >= 2) listOf(GraphShape.Polyline(points, expression.color)) else emptyList()
    }
}
