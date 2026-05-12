package com.cloveriris.calcore.presentation.linearalgebra

import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.domain.model.matrix.MatrixResult
import com.cloveriris.calcore.domain.model.matrix.MatrixStep

/**
 * 线性代数工作台 UI 状态
 */
data class LinearAlgebraUiState(
    val matrices: List<Matrix<Double>> = emptyList(),
    val selectedMatrixIndex: Int = -1,
    val secondarySelectedIndex: Int = -1, // 用于二元运算选择第二个矩阵
    val operation: MatrixOperation = MatrixOperation.NONE,
    val result: MatrixResult? = null,
    val scalarResult: Double? = null,      // 行列式等标量结果
    val steps: List<MatrixStep> = emptyList(),
    val currentStepIndex: Int = -1,
    val isAnimating: Boolean = false,
    val animationProgress: Float = 0f,
    val showVectorTransform: Boolean = false,
    val vectorTransformMatrix: Matrix<Double>? = null, // 用于2D向量变换动画的2x2矩阵
    val errorMessage: String? = null
)

enum class MatrixOperation {
    NONE, ADD, MULTIPLY, TRANSPOSE, DETERMINANT, INVERSE, EIGENVALUE
}
