package com.cloveriris.calcore.domain.model.matrix

/**
 * 通用矩阵数据模型
 *
 * 支持任意尺寸和元素类型，默认使用 Double。
 * 为 Compose UI 提供不可变快照，所有运算返回新实例。
 */
data class Matrix<T : Number>(
    val rows: Int,
    val cols: Int,
    val data: List<List<T>>,
    val name: String = "A"
) {
    init {
        require(data.size == rows) { "Row count mismatch: ${data.size} != $rows" }
        require(data.all { it.size == cols }) { "Column count mismatch" }
    }

    operator fun get(row: Int, col: Int): T = data[row][col]

    fun row(index: Int): List<T> = data[index]
    fun col(index: Int): List<T> = data.map { it[index] }

    fun map(transform: (T) -> Double): Matrix<Double> = Matrix(
        rows, cols,
        data.map { row -> row.map { transform(it).toDouble() } },
        name
    )

    companion object {
        fun identity(size: Int): Matrix<Double> = Matrix(
            size, size,
            List(size) { r ->
                List(size) { c ->
                    if (r == c) 1.0 else 0.0
                }
            },
            "I"
        )

        fun zeros(rows: Int, cols: Int): Matrix<Double> = Matrix(
            rows, cols,
            List(rows) { List(cols) { 0.0 } },
            "O"
        )
    }
}

/**
 * 向量（单列矩阵的简化表示）
 */
data class Vector(
    val components: List<Double>,
    val name: String = "v"
) {
    val dimension: Int get() = components.size
    operator fun get(index: Int): Double = components[index]
}

/**
 * 矩阵运算结果封装
 */
sealed class MatrixResult {
    data class Success(
        val result: Matrix<Double>,
        val steps: List<MatrixStep> = emptyList()
    ) : MatrixResult()

    data class Error(val message: String) : MatrixResult()
}

/**
 * 矩阵运算的单个步骤（用于逐步动画）
 */
data class MatrixStep(
    val description: String,
    val highlightRow: Int? = null,
    val highlightCol: Int? = null,
    val highlightCells: List<Pair<Int, Int>> = emptyList(),
    val intermediateValue: Double? = null
)
