package com.cloveriris.calcore.engine.matrix

import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.domain.model.matrix.MatrixResult
import com.cloveriris.calcore.domain.model.matrix.MatrixStep
import kotlin.math.abs

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

    /**
     * 行列式（高斯消元法：化为上三角后对角线乘积）
     */
    fun determinant(m: Matrix<Double>): Double? {
        if (m.rows != m.cols) return null
        val n = m.rows
        // 复制为可变二维数组
        val a = Array(n) { r -> DoubleArray(n) { c -> m[r, c] } }
        var det = 1.0
        var swaps = 0

        for (i in 0 until n) {
            // 选主元（部分主元消去）
            var pivotRow = i
            var maxVal = abs(a[i][i])
            for (r in i + 1 until n) {
                if (abs(a[r][i]) > maxVal) {
                    maxVal = abs(a[r][i])
                    pivotRow = r
                }
            }
            if (maxVal < 1e-12) return 0.0 // 奇异矩阵

            if (pivotRow != i) {
                val temp = a[i]
                a[i] = a[pivotRow]
                a[pivotRow] = temp
                swaps++
            }

            val pivot = a[i][i]
            det *= pivot

            for (r in i + 1 until n) {
                val factor = a[r][i] / pivot
                for (c in i until n) {
                    a[r][c] -= factor * a[i][c]
                }
            }
        }

        if (swaps % 2 != 0) det = -det
        return det
    }

    /**
     * 逆矩阵（Gauss-Jordan 消元法：[A|I] → [I|A⁻¹]）
     */
    fun inverse(m: Matrix<Double>): MatrixResult {
        if (m.rows != m.cols) {
            return MatrixResult.Error("Only square matrices have inverses")
        }
        val n = m.rows
        // 构造增广矩阵 [A | I]
        val aug = Array(n) { r ->
            DoubleArray(2 * n) { c ->
                when {
                    c < n -> m[r, c]
                    c == n + r -> 1.0
                    else -> 0.0
                }
            }
        }
        val steps = mutableListOf<MatrixStep>()

        for (i in 0 until n) {
            // 选主元
            var pivotRow = i
            var maxVal = abs(aug[i][i])
            for (r in i + 1 until n) {
                if (abs(aug[r][i]) > maxVal) {
                    maxVal = abs(aug[r][i])
                    pivotRow = r
                }
            }
            if (maxVal < 1e-12) {
                return MatrixResult.Error("Matrix is singular (not invertible)")
            }

            if (pivotRow != i) {
                val temp = aug[i]
                aug[i] = aug[pivotRow]
                aug[pivotRow] = temp
                steps.add(MatrixStep(description = "Swap R$i ↔ R$pivotRow"))
            }

            // 归一化主元行
            val pivot = aug[i][i]
            for (c in 0 until 2 * n) {
                aug[i][c] /= pivot
            }
            steps.add(MatrixStep(description = "R$i ← R$i / ${format(pivot)}", highlightRow = i))

            // 消去其他行
            for (r in 0 until n) {
                if (r == i) continue
                val factor = aug[r][i]
                if (abs(factor) > 1e-12) {
                    for (c in 0 until 2 * n) {
                        aug[r][c] -= factor * aug[i][c]
                    }
                    steps.add(MatrixStep(
                        description = "R$r ← R$r - ${format(factor)}·R$i",
                        highlightRow = r,
                        highlightCol = i
                    ))
                }
            }
        }

        val invData = List(n) { r ->
            List(n) { c -> aug[r][n + c] }
        }
        val result = Matrix(n, n, invData, "${m.name}⁻¹")
        return MatrixResult.Success(result, steps)
    }

    /**
     * 幂法求最大模特征值及对应特征向量
     *
     * @return Pair<特征值, 特征向量>，若矩阵非方阵返回 null
     */
    fun dominantEigenvalue(m: Matrix<Double>): Pair<Double, List<Double>>? {
        if (m.rows != m.cols) return null
        val n = m.rows
        // 随机初始向量（避免与特征向量正交）
        var vec = List(n) { 1.0 }
        var eigenvalue = 0.0

        repeat(100) {
            // 矩阵-向量乘法
            val newVec = List(n) { r ->
                (0 until n).sumOf { c -> m[r, c] * vec[c] }
            }
            // 归一化
            val norm = kotlin.math.sqrt(newVec.sumOf { it * it })
            if (norm < 1e-12) return null
            vec = newVec.map { it / norm }
            // Rayleigh 商近似特征值
            eigenvalue = (0 until n).sumOf { r ->
                vec[r] * (0 until n).sumOf { c -> m[r, c] * vec[c] }
            }
        }
        return eigenvalue to vec
    }

    private fun format(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.3f".format(v).trimEnd('0').trimEnd('.')
    }
}
