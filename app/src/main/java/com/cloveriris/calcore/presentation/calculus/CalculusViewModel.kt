package com.cloveriris.calcore.presentation.calculus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.calculus.*
import com.cloveriris.calcore.engine.calculus.NumericalCalculus
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
class CalculusViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CalculusUiState())
    val uiState: StateFlow<CalculusUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null

    fun setMode(mode: CalculusMode) {
        playbackJob?.cancel()
        _uiState.update { it.copy(mode = mode, isPlaying = false, currentFrameIndex = 0) }
        recalc()
    }

    fun setFunction(expr: String) {
        _uiState.update { it.copy(function = expr) }
        recalc()
    }

    fun setPoint(p: Double) {
        _uiState.update { it.copy(point = p) }
        recalc()
    }

    fun setBounds(lower: Double, upper: Double) {
        _uiState.update { it.copy(lowerBound = lower, upperBound = upper) }
        recalc()
    }

    fun setCenter(c: Double) {
        _uiState.update { it.copy(center = c) }
        recalc()
    }

    fun setOrder(o: Int) {
        _uiState.update { it.copy(order = o.coerceIn(1, 10)) }
        recalc()
    }

    fun setSubdivisions(n: Int) {
        _uiState.update { it.copy(subdivisions = n.coerceAtLeast(2)) }
        recalc()
    }

    fun setIntegrationMethod(method: IntegrationMethod) {
        _uiState.update { it.copy(integrationMethod = method) }
        recalc()
    }

    fun setViewport(minX: Double, maxX: Double, minY: Double, maxY: Double) {
        _uiState.update { it.copy(viewport = CalculusViewport(minX, maxX, minY, maxY)) }
    }

    fun playPause() {
        val current = _uiState.value
        if (current.isPlaying) {
            playbackJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            if (current.currentFrameIndex >= current.frames.lastIndex) {
                _uiState.update { it.copy(currentFrameIndex = 0) }
            }
            startPlayback()
        }
    }

    fun setFrameIndex(index: Int) {
        playbackJob?.cancel()
        _uiState.update {
            it.copy(
                currentFrameIndex = index.coerceIn(0, it.frames.lastIndex.coerceAtLeast(0)),
                isPlaying = false
            )
        }
    }

    fun resetView() {
        _uiState.update { it.copy(viewport = CalculusViewport()) }
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

    private fun recalc() {
        playbackJob?.cancel()
        val state = _uiState.value
        when (state.mode) {
            CalculusMode.LIMIT -> {
                val frames = NumericalCalculus.limitFrames(
                    LimitConfig(state.function, state.point, state.point * state.point)
                )
                _uiState.update { it.copy(frames = frames, steps = emptyList(), currentFrameIndex = 0, isPlaying = false) }
            }
            CalculusMode.DERIVATIVE -> {
                val result = NumericalCalculus.derivative(
                    DerivativeConfig(state.function, state.point)
                )
                _uiState.update {
                    it.copy(
                        frames = result.geometryFrames,
                        steps = result.steps,
                        resultValue = result.slope,
                        currentFrameIndex = 0,
                        isPlaying = false
                    )
                }
            }
            CalculusMode.INTEGRAL -> {
                val result = NumericalCalculus.integrate(
                    IntegrationConfig(
                        function = state.function,
                        lowerBound = state.lowerBound,
                        upperBound = state.upperBound,
                        method = state.integrationMethod,
                        subdivisions = state.subdivisions
                    )
                )
                _uiState.update {
                    it.copy(
                        frames = result.geometryFrames,
                        steps = result.steps,
                        resultValue = result.value,
                        currentFrameIndex = 0,
                        isPlaying = false
                    )
                }
            }
            CalculusMode.TAYLOR -> {
                val result = NumericalCalculus.taylorExpansion(
                    TaylorConfig(state.function, state.center, state.order)
                )
                _uiState.update {
                    it.copy(
                        frames = result.geometryFrames,
                        steps = result.steps,
                        currentFrameIndex = 0,
                        isPlaying = false
                    )
                }
            }
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            val frameCount = _uiState.value.frames.size
            if (frameCount <= 1) {
                _uiState.update { it.copy(isPlaying = false) }
                return@launch
            }
            val durationPerFrame = 1200L / frameCount
            while (_uiState.value.isPlaying && _uiState.value.currentFrameIndex < frameCount - 1) {
                delay(durationPerFrame)
                _uiState.update { current ->
                    val next = (current.currentFrameIndex + 1).coerceAtMost(current.frames.lastIndex)
                    current.copy(currentFrameIndex = next)
                }
            }
            _uiState.update { it.copy(isPlaying = false) }
        }
    }
}
