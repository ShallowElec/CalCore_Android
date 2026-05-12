package com.cloveriris.calcore.presentation.linearalgebra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.domain.model.matrix.MatrixResult
import com.cloveriris.calcore.engine.matrix.MatrixOperations
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
class LinearAlgebraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LinearAlgebraUiState())
    val uiState: StateFlow<LinearAlgebraUiState> = _uiState.asStateFlow()

    private var animationJob: Job? = null

    init {
        // 预置 2 个默认矩阵，方便首次进入即有内容
        val presets = listOf(
            Matrix(
                2, 2,
                listOf(
                    listOf(2.0, 1.0),
                    listOf(1.0, 3.0)
                ),
                "A"
            ),
            Matrix(
                2, 2,
                listOf(
                    listOf(1.0, 0.0),
                    listOf(0.0, 1.0)
                ),
                "I"
            )
        )
        _uiState.update { it.copy(matrices = presets, selectedMatrixIndex = 0) }
    }

    fun selectMatrix(index: Int) {
        _uiState.update {
            it.copy(
                selectedMatrixIndex = index,
                result = null,
                scalarResult = null,
                steps = emptyList(),
                currentStepIndex = -1,
                showVectorTransform = false,
                operation = MatrixOperation.NONE,
                errorMessage = null
            )
        }
    }

    fun selectSecondaryMatrix(index: Int) {
        _uiState.update { it.copy(secondarySelectedIndex = index) }
    }

    fun addMatrix(rows: Int = 2, cols: Int = 2) {
        val name = generateMatrixName()
        val newMatrix = Matrix(rows, cols, List(rows) { List(cols) { 0.0 } }, name)
        _uiState.update { current ->
            current.copy(
                matrices = current.matrices + newMatrix,
                selectedMatrixIndex = current.matrices.size
            )
        }
    }

    fun removeMatrix(index: Int) {
        _uiState.update { current ->
            val newMatrices = current.matrices.filterIndexed { i, _ -> i != index }
            val newSelected = when {
                newMatrices.isEmpty() -> -1
                current.selectedMatrixIndex == index -> 0
                current.selectedMatrixIndex > index -> current.selectedMatrixIndex - 1
                else -> current.selectedMatrixIndex
            }
            val newSecondary = when {
                current.secondarySelectedIndex == index -> -1
                current.secondarySelectedIndex > index -> current.secondarySelectedIndex - 1
                else -> current.secondarySelectedIndex
            }
            current.copy(
                matrices = newMatrices,
                selectedMatrixIndex = newSelected,
                secondarySelectedIndex = newSecondary
            )
        }
    }

    fun updateCell(matrixIndex: Int, row: Int, col: Int, value: Double) {
        _uiState.update { current ->
            val matrix = current.matrices.getOrNull(matrixIndex) ?: return@update current
            if (row >= matrix.rows || col >= matrix.cols) return@update current
            val newData = matrix.data.mapIndexed { r, rowData ->
                rowData.mapIndexed { c, v ->
                    if (r == row && c == col) value else v
                }
            }
            val newMatrix = matrix.copy(data = newData)
            val newMatrices = current.matrices.toMutableList().apply {
                set(matrixIndex, newMatrix)
            }
            current.copy(matrices = newMatrices)
        }
    }

    fun addRow(matrixIndex: Int) {
        _uiState.update { current ->
            val matrix = current.matrices.getOrNull(matrixIndex) ?: return@update current
            val newData = matrix.data + listOf(List(matrix.cols) { 0.0 })
            val newMatrix = matrix.copy(rows = matrix.rows + 1, data = newData)
            val newMatrices = current.matrices.toMutableList().apply { set(matrixIndex, newMatrix) }
            current.copy(matrices = newMatrices)
        }
    }

    fun removeRow(matrixIndex: Int) {
        _uiState.update { current ->
            val matrix = current.matrices.getOrNull(matrixIndex) ?: return@update current
            if (matrix.rows <= 1) return@update current
            val newData = matrix.data.dropLast(1)
            val newMatrix = matrix.copy(rows = matrix.rows - 1, data = newData)
            val newMatrices = current.matrices.toMutableList().apply { set(matrixIndex, newMatrix) }
            current.copy(matrices = newMatrices)
        }
    }

    fun addCol(matrixIndex: Int) {
        _uiState.update { current ->
            val matrix = current.matrices.getOrNull(matrixIndex) ?: return@update current
            val newData = matrix.data.map { it + 0.0 }
            val newMatrix = matrix.copy(cols = matrix.cols + 1, data = newData)
            val newMatrices = current.matrices.toMutableList().apply { set(matrixIndex, newMatrix) }
            current.copy(matrices = newMatrices)
        }
    }

    fun removeCol(matrixIndex: Int) {
        _uiState.update { current ->
            val matrix = current.matrices.getOrNull(matrixIndex) ?: return@update current
            if (matrix.cols <= 1) return@update current
            val newData = matrix.data.map { it.dropLast(1) }
            val newMatrix = matrix.copy(cols = matrix.cols - 1, data = newData)
            val newMatrices = current.matrices.toMutableList().apply { set(matrixIndex, newMatrix) }
            current.copy(matrices = newMatrices)
        }
    }

    fun performOperation(operation: MatrixOperation) {
        animationJob?.cancel()
        val current = _uiState.value
        val primaryIdx = current.selectedMatrixIndex
        if (primaryIdx < 0 || primaryIdx >= current.matrices.size) {
            _uiState.update { it.copy(errorMessage = "Please select a matrix first") }
            return
        }
        val primary = current.matrices[primaryIdx]

        when (operation) {
            MatrixOperation.ADD -> {
                val secIdx = current.secondarySelectedIndex
                if (secIdx < 0 || secIdx >= current.matrices.size) {
                    _uiState.update { it.copy(errorMessage = "Please select a second matrix for addition") }
                    return
                }
                val secondary = current.matrices[secIdx]
                val result = MatrixOperations.add(primary, secondary)
                handleResult(result, operation)
            }
            MatrixOperation.MULTIPLY -> {
                val secIdx = current.secondarySelectedIndex
                if (secIdx < 0 || secIdx >= current.matrices.size) {
                    _uiState.update { it.copy(errorMessage = "Please select a second matrix for multiplication") }
                    return
                }
                val secondary = current.matrices[secIdx]
                val result = MatrixOperations.multiply(primary, secondary)
                handleResult(result, operation)
            }
            MatrixOperation.TRANSPOSE -> {
                val result = MatrixOperations.transpose(primary)
                _uiState.update {
                    it.copy(
                        operation = operation,
                        result = MatrixResult.Success(result),
                        scalarResult = null,
                        steps = emptyList(),
                        currentStepIndex = -1,
                        showVectorTransform = false,
                        errorMessage = null
                    )
                }
            }
            MatrixOperation.DETERMINANT -> {
                val det = MatrixOperations.determinant(primary)
                _uiState.update {
                    it.copy(
                        operation = operation,
                        result = null,
                        scalarResult = det,
                        steps = emptyList(),
                        currentStepIndex = -1,
                        showVectorTransform = false,
                        errorMessage = if (det == null) "Matrix must be square" else null
                    )
                }
            }
            MatrixOperation.INVERSE -> {
                val result = MatrixOperations.inverse(primary)
                handleResult(result, operation)
            }
            MatrixOperation.EIGENVALUE -> {
                val eigen = MatrixOperations.dominantEigenvalue(primary)
                _uiState.update {
                    it.copy(
                        operation = operation,
                        result = null,
                        scalarResult = eigen?.first,
                        steps = emptyList(),
                        currentStepIndex = -1,
                        showVectorTransform = false,
                        errorMessage = if (eigen == null) "Failed to compute eigenvalue" else null
                    )
                }
            }
            MatrixOperation.NONE -> { /* do nothing */ }
        }
    }

    private fun handleResult(result: MatrixResult, operation: MatrixOperation) {
        when (result) {
            is MatrixResult.Success -> {
                _uiState.update {
                    it.copy(
                        operation = operation,
                        result = result,
                        scalarResult = null,
                        steps = result.steps,
                        currentStepIndex = -1,
                        showVectorTransform = false,
                        errorMessage = null
                    )
                }
                // 如果是 2x2 矩阵乘法，自动显示向量变换动画
                val matrix = result.result
                if (operation == MatrixOperation.MULTIPLY && matrix.rows == 2 && matrix.cols == 2) {
                    _uiState.update { it.copy(showVectorTransform = true, vectorTransformMatrix = matrix) }
                }
                if (result.steps.isNotEmpty()) {
                    startStepAnimation()
                }
            }
            is MatrixResult.Error -> {
                _uiState.update {
                    it.copy(
                        operation = operation,
                        result = null,
                        scalarResult = null,
                        steps = emptyList(),
                        currentStepIndex = -1,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun startStepAnimation() {
        animationJob?.cancel()
        val steps = _uiState.value.steps
        if (steps.isEmpty()) return
        animationJob = viewModelScope.launch {
            _uiState.update { it.copy(isAnimating = true, currentStepIndex = 0, animationProgress = 0f) }
            for (i in steps.indices) {
                _uiState.update { it.copy(currentStepIndex = i, animationProgress = (i + 1f) / steps.size) }
                delay(600)
            }
            _uiState.update { it.copy(isAnimating = false) }
        }
    }

    fun pauseAnimation() {
        animationJob?.cancel()
        _uiState.update { it.copy(isAnimating = false) }
    }

    fun resetAnimation() {
        animationJob?.cancel()
        _uiState.update { it.copy(currentStepIndex = -1, animationProgress = 0f, isAnimating = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun generateMatrixName(): String {
        val existing = _uiState.value.matrices.map { it.name }.toSet()
        var idx = _uiState.value.matrices.size
        var name: String
        do {
            name = "M${idx + 1}"
            idx++
        } while (name in existing)
        return name
    }
}
