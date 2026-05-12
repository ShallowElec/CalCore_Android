package com.cloveriris.calcore.ui.linearalgebra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.domain.model.matrix.MatrixResult
import com.cloveriris.calcore.domain.model.matrix.MatrixStep
import com.cloveriris.calcore.presentation.linearalgebra.MatrixOperation
import com.cloveriris.calcore.ui.theme.CalcoreTheme

/**
 * 结果展示面板
 *
 * 显示矩阵运算结果、标量结果、步骤列表及动画控制。
 */
@Composable
fun ResultPanel(
    result: MatrixResult?,
    scalarResult: Double?,
    operation: MatrixOperation,
    steps: List<MatrixStep>,
    currentStepIndex: Int,
    isAnimating: Boolean,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = if (steps.isNotEmpty()) {
        ((currentStepIndex + 1).coerceAtLeast(0)).toFloat() / steps.size
    } else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Result",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (operation != MatrixOperation.NONE) {
            Text(
                text = operation.displayName(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            result is MatrixResult.Success -> {
                ResultMatrixGrid(matrix = result.result)
            }

            scalarResult != null -> {
                ScalarResultDisplay(
                    operation = operation,
                    value = scalarResult
                )
            }

            result is MatrixResult.Error -> {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select an operation to see the result",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Steps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                val isCurrentStep = index == currentStepIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isCurrentStep) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animationProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isAnimating) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAnimating) "Pause" else "Play",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reset",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultMatrixGrid(
    matrix: Matrix<Double>,
    modifier: Modifier = Modifier
) {
    val cellSize = 44.dp
    val cellTextSize = 13.sp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Optional matrix name label
        if (matrix.name.isNotBlank()) {
            Text(
                text = matrix.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(1.dp)
        ) {
            matrix.data.forEach { row ->
                Row {
                    row.forEach { value ->
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatMatrixValue(value),
                                fontSize = cellTextSize,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "${matrix.rows}×${matrix.cols}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ScalarResultDisplay(
    operation: MatrixOperation,
    value: Double,
    modifier: Modifier = Modifier
) {
    val label = when (operation) {
        MatrixOperation.DETERMINANT -> "det(A) ="
        MatrixOperation.EIGENVALUE -> "λ_max ="
        else -> "Result ="
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = formatMatrixValue(value),
            style = MaterialTheme.typography.displayMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

private fun MatrixOperation.displayName(): String = when (this) {
    MatrixOperation.NONE -> ""
    MatrixOperation.ADD -> "Matrix Addition"
    MatrixOperation.MULTIPLY -> "Matrix Multiplication"
    MatrixOperation.TRANSPOSE -> "Transpose"
    MatrixOperation.DETERMINANT -> "Determinant"
    MatrixOperation.INVERSE -> "Inverse"
    MatrixOperation.EIGENVALUE -> "Eigenvalue"
}

private fun formatMatrixValue(value: Double): String {
    return when {
        value == 0.0 -> "0"
        value == value.toLong().toDouble() -> value.toLong().toString()
        kotlin.math.abs(value) < 0.001 || kotlin.math.abs(value) >= 10000 -> {
            "%.3e".format(value)
        }
        else -> {
            val formatted = "%.3f".format(value)
            if (formatted.contains('.')) {
                formatted.trimEnd('0').trimEnd('.')
            } else formatted
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultPanelMatrixPreview() {
    CalcoreTheme {
        ResultPanel(
            result = MatrixResult.Success(
                Matrix(2, 2, listOf(listOf(3.0, 1.0), listOf(1.0, 4.0)), "A+B"),
                steps = listOf(
                    MatrixStep("C[0,0] = A[0,0] + B[0,0]"),
                    MatrixStep("C[0,1] = A[0,1] + B[0,1]")
                )
            ),
            scalarResult = null,
            operation = MatrixOperation.ADD,
            steps = listOf(
                MatrixStep("C[0,0] = A[0,0] + B[0,0]"),
                MatrixStep("C[0,1] = A[0,1] + B[0,1]")
            ),
            currentStepIndex = 0,
            isAnimating = true,
            onPlayPause = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultPanelScalarPreview() {
    CalcoreTheme {
        ResultPanel(
            result = null,
            scalarResult = 5.0,
            operation = MatrixOperation.DETERMINANT,
            steps = emptyList(),
            currentStepIndex = -1,
            isAnimating = false,
            onPlayPause = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultPanelEmptyPreview() {
    CalcoreTheme {
        ResultPanel(
            result = null,
            scalarResult = null,
            operation = MatrixOperation.NONE,
            steps = emptyList(),
            currentStepIndex = -1,
            isAnimating = false,
            onPlayPause = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultPanelErrorPreview() {
    CalcoreTheme {
        ResultPanel(
            result = MatrixResult.Error("Dimension mismatch: 2x3 vs 3x2"),
            scalarResult = null,
            operation = MatrixOperation.ADD,
            steps = emptyList(),
            currentStepIndex = -1,
            isAnimating = false,
            onPlayPause = {},
            onReset = {}
        )
    }
}
