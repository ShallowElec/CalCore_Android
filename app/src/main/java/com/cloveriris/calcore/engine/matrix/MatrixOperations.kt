package com.cloveriris.calcore.engine.matrix

import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.domain.model.matrix.MatrixResult
import com.cloveriris.calcore.domain.model.matrix.MatrixStep

/**
 * 矩阵运算引擎
 *
 * 纯算法层，无 Android 依赖。所有运算在后台协程执行。
 */
object MatrixOperations {

    fun add(a: Matrix<Double>, b: Matrix<Double>): MatrixResult {
        if (a.rows != b.rows || a.cols != b.cols) {
            return MatrixResult.Error("Dimension mismatch: ${a.rows}x${a.cols} vs ${b.rows}x${b.cols}")
        }
        val result = Matrix(
            a.rows, a.cols,
            List(a.rows) { r ->
                List(a.cols) { c ->
                    a[r, c] + b[r, c]
                }
            },
            "${a.name}+${b.name}"
        )
        return MatrixResult.Success(result)
    }

    fun multiply(a: Matrix<Double>, b: Matrix<Double>): MatrixResult {
        if (a.cols != b.rows) {
            return MatrixResult.Error("Cannot multiply: ${a.rows}x${a.cols} · ${b.rows}x${b.cols}")
        }
        val steps = mutableListOf<MatrixStep>()
        val result = Matrix(
            a.rows, b.cols,
            List(a.rows) { r ->
                List(b.cols) { c ->
                    var sum = 0.0
                    for (k in 0 until a.cols) {
                        sum += a[r, k] * b[k, c]
                    }
                    steps.add(
                        MatrixStep(
                            description = "C[$r,$c] = Σ A[$r,k]·B[k,$c]",
                            highlightCells = listOf(r to c),
                            intermediateValue = sum
                        )
                    )
                    sum
                }
            },
            "${a.name}·${b.name}"
        )
        return MatrixResult.Success(result, steps)
    }

    fun transpose(m: Matrix<Double>): Matrix<Double> = Matrix(
        m.cols, m.rows,
        List(m.cols) { c ->
            List(m.rows) { r ->
                m[r, c]
            }
        },
        "${m.name}ᵀ"
    )

    fun determinant(m: Matrix<Double>): Double? {
        if (m.rows != m.cols) return null
        // TODO: 高斯消元或拉普拉斯展开
        return 0.0
    }

    fun inverse(m: Matrix<Double>): MatrixResult {
        if (m.rows != m.cols) {
            return MatrixResult.Error("Only square matrices have inverses")
        }
        // TODO: Gauss-Jordan 消元
        return MatrixResult.Error("Not implemented")
    }
}
