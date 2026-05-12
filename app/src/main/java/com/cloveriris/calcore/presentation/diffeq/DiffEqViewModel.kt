package com.cloveriris.calcore.presentation.diffeq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.equation.*
import com.cloveriris.calcore.engine.ode.OdeSolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiffEqViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DiffEqUiState())
    val uiState: StateFlow<DiffEqUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null

    val templates = listOf(
        OdeTemplate(
            id = "exponential_decay",
            name = "指数衰减",
            description = "dy/dt = -λy",
            equation = "dy/dt = -λy",
            variableCount = 1,
            variableNames = listOf("y"),
            defaultParameters = mapOf("lambda" to 0.5),
            defaultInitial = mapOf("y" to 1.0),
            defaultTimeEnd = 10.0,
            dydt = { _, y, params ->
                listOf(-(params["lambda"] ?: 0.5) * y[0])
            }
        ),
        OdeTemplate(
            id = "harmonic_oscillator",
            name = "简谐振动",
            description = "d²x/dt² + ω²x = 0",
            equation = "dx/dt = v, dv/dt = -ω²x",
            variableCount = 2,
            variableNames = listOf("x", "v"),
            defaultParameters = mapOf("omega" to 2.0),
            defaultInitial = mapOf("x" to 1.0, "v" to 0.0),
            defaultTimeEnd = 10.0,
            dydt = { _, y, params ->
                val omega = params["omega"] ?: 2.0
                listOf(y[1], -omega * omega * y[0])
            }
        ),
        OdeTemplate(
            id = "lotka_volterra",
            name = "捕食者-猎物",
            description = "Lotka-Volterra 模型",
            equation = "dx/dt = αx-βxy, dy/dt = δxy-γy",
            variableCount = 2,
            variableNames = listOf(" prey", "pred"),
            defaultParameters = mapOf("alpha" to 1.0, "beta" to 0.5, "gamma" to 0.75, "delta" to 0.25),
            defaultInitial = mapOf("prey" to 2.0, "pred" to 1.0),
            defaultTimeEnd = 30.0,
            dydt = { _, y, params ->
                val alpha = params["alpha"] ?: 1.0
                val beta = params["beta"] ?: 0.5
                val gamma = params["gamma"] ?: 0.75
                val delta = params["delta"] ?: 0.25
                listOf(
                    alpha * y[0] - beta * y[0] * y[1],
                    delta * y[0] * y[1] - gamma * y[1]
                )
            }
        )
    )

    init {
        selectTemplate(0)
    }

    fun selectTemplate(index: Int) {
        playbackJob?.cancel()
        val template = templates.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                selectedTemplateIndex = index,
                parameters = template.defaultParameters,
                timeEnd = template.defaultTimeEnd,
                currentTimeIndex = 0,
                isPlaying = false
            )
        }
        solve()
    }

    fun setSolverMethod(method: SolverMethod) {
        _uiState.update { it.copy(solverMethod = method) }
        solve()
    }

    fun setTimeStep(dt: Double) {
        _uiState.update { it.copy(timeStep = dt.coerceIn(0.001, 1.0)) }
        solve()
    }

    fun setParameter(name: String, value: Double) {
        _uiState.update {
            it.copy(parameters = it.parameters.toMutableMap().apply { put(name, value) })
        }
        solve()
    }

    fun setTimeEnd(end: Double) {
        _uiState.update { it.copy(timeEnd = end.coerceAtLeast(1.0)) }
        solve()
    }

    fun toggleDirectionField() {
        _uiState.update { it.copy(showDirectionField = !it.showDirectionField) }
    }

    fun togglePhasePortrait() {
        _uiState.update { it.copy(showPhasePortrait = !it.showPhasePortrait) }
    }

    fun setViewport(minX: Double, maxX: Double, minY: Double, maxY: Double) {
        _uiState.update { it.copy(viewport = DiffEqViewport(minX, maxX, minY, maxY)) }
    }

    fun resetViewport() {
        val template = templates.getOrNull(_uiState.value.selectedTemplateIndex)
        _uiState.update {
            it.copy(viewport = DiffEqViewport(
                minX = -1.0,
                maxX = it.timeEnd,
                minY = if (template?.variableCount == 2) -3.0 else -2.0,
                maxY = if (template?.variableCount == 2) 3.0 else 2.0
            ))
        }
    }

    fun panViewport(dxWorld: Double, dyWorld: Double) {
        _uiState.update {
            val vp = it.viewport
            it.copy(viewport = vp.copy(
                minX = vp.minX - dxWorld,
                maxX = vp.maxX - dxWorld,
                minY = vp.minY - dyWorld,
                maxY = vp.maxY - dyWorld
            ))
        }
    }

    fun zoomViewport(zoomFactor: Double, anchorWorldX: Double, anchorWorldY: Double) {
        _uiState.update {
            val vp = it.viewport
            val rangeX = vp.maxX - vp.minX
            val rangeY = vp.maxY - vp.minY
            val newRangeX = rangeX / zoomFactor
            val newRangeY = rangeY / zoomFactor
            val ratioX = (anchorWorldX - vp.minX) / rangeX
            val ratioY = (anchorWorldY - vp.minY) / rangeY
            it.copy(viewport = vp.copy(
                minX = anchorWorldX - newRangeX * ratioX,
                maxX = anchorWorldX + newRangeX * (1 - ratioX),
                minY = anchorWorldY - newRangeY * ratioY,
                maxY = anchorWorldY + newRangeY * (1 - ratioY)
            ))
        }
    }

    fun playPause() {
        val current = _uiState.value
        if (current.isPlaying) {
            playbackJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            val sol = current.solution
            if (sol == null || sol.timePoints.size <= 1) return
            if (current.currentTimeIndex >= sol.timePoints.lastIndex) {
                _uiState.update { it.copy(currentTimeIndex = 0) }
            }
            startPlayback()
        }
    }

    fun setTimeIndex(index: Int) {
        playbackJob?.cancel()
        _uiState.update {
            val maxIndex = it.solution?.timePoints?.lastIndex ?: 0
            it.copy(currentTimeIndex = index.coerceIn(0, maxIndex), isPlaying = false)
        }
    }

    private fun solve() {
        playbackJob?.cancel()
        val state = _uiState.value
        val template = templates.getOrNull(state.selectedTemplateIndex) ?: return

        val ode = OdeDefinition(
            type = if (template.variableCount == 1) OdeType.FIRST_ORDER_SCALAR else OdeType.FIRST_ORDER_SYSTEM,
            equation = template.equation,
            variables = template.variableNames,
            parameters = state.parameters,
            initialConditions = template.defaultInitial,
            timeRange = 0.0..state.timeEnd,
            solverConfig = SolverConfig(
                method = state.solverMethod,
                timeStep = state.timeStep
            )
        )

        val solution = OdeSolver.solve(ode) { t, y ->
            template.dydt(t, y, state.parameters)
        }
        _uiState.update {
            it.copy(
                solution = solution,
                currentTimeIndex = 0,
                isPlaying = false
            )
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            val pointCount = _uiState.value.solution?.timePoints?.size ?: 0
            if (pointCount <= 1) {
                _uiState.update { it.copy(isPlaying = false) }
                return@launch
            }
            val durationPerPoint = 8000L / pointCount
            while (_uiState.value.isPlaying) {
                delay(durationPerPoint)
                _uiState.update { current ->
                    val next = current.currentTimeIndex + 1
                    if (next >= pointCount) {
                        current.copy(isPlaying = false, currentTimeIndex = pointCount - 1)
                    } else {
                        current.copy(currentTimeIndex = next)
                    }
                }
            }
        }
    }
}
