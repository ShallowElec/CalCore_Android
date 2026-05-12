package com.cloveriris.calcore.ui.linearalgebra

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.presentation.linearalgebra.MatrixOperation
import com.cloveriris.calcore.ui.theme.CalcoreTheme

/**
 * 运算工具栏
 *
 * 提供矩阵二元运算（加、乘）和一元运算（转置、行列式、逆矩阵、特征值）的选择。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperationToolbar(
    matrices: List<Matrix<Double>>,
    selectedIndex: Int,
    secondaryIndex: Int,
    onSelectSecondary: (Int) -> Unit,
    onOperation: (MatrixOperation) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMatrix = matrices.getOrNull(selectedIndex)
    val isSquare = selectedMatrix?.let { it.rows == it.cols } ?: false

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Operations",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Binary Operations
        Text(
            text = "Binary Operations",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Second matrix:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (matrices.size < 2) {
            Text(
                text = "Need at least 2 matrices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                matrices.forEachIndexed { index, matrix ->
                    FilterChip(
                        selected = index == secondaryIndex,
                        onClick = { onSelectSecondary(index) },
                        label = {
                            Text("${matrix.name}  ${matrix.rows}×${matrix.cols}")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val secondarySelected = secondaryIndex in matrices.indices

        Button(
            onClick = { onOperation(MatrixOperation.ADD) },
            modifier = Modifier.fillMaxWidth(),
            enabled = secondarySelected
        ) {
            Text("Add (A+B)")
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { onOperation(MatrixOperation.MULTIPLY) },
            modifier = Modifier.fillMaxWidth(),
            enabled = secondarySelected
        ) {
            Text("Multiply (A·B)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Unary Operations
        Text(
            text = "Unary Operations",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            OutlinedButton(
                onClick = { onOperation(MatrixOperation.TRANSPOSE) },
                modifier = Modifier.weight(1f),
                enabled = selectedMatrix != null
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("Transpose")
                    Text(
                        "Aᵀ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = { onOperation(MatrixOperation.DETERMINANT) },
                modifier = Modifier.weight(1f),
                enabled = isSquare
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("Determinant")
                    Text(
                        "det(A)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = { onOperation(MatrixOperation.INVERSE) },
                modifier = Modifier.weight(1f),
                enabled = isSquare
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("Inverse")
                    Text(
                        "A⁻¹",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = { onOperation(MatrixOperation.EIGENVALUE) },
                modifier = Modifier.weight(1f),
                enabled = isSquare
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("Eigenvalue")
                    Text(
                        "λ_max",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OperationToolbarPreview() {
    CalcoreTheme {
        OperationToolbar(
            matrices = listOf(
                Matrix(2, 2, listOf(listOf(1.0, 0.0), listOf(0.0, 1.0)), "A"),
                Matrix(2, 2, listOf(listOf(2.0, 1.0), listOf(1.0, 3.0)), "B"),
                Matrix(3, 2, listOf(listOf(1.0, 0.0), listOf(0.0, 1.0), listOf(1.0, 1.0)), "C")
            ),
            selectedIndex = 0,
            secondaryIndex = 1,
            onSelectSecondary = {},
            onOperation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OperationToolbarEmptyPreview() {
    CalcoreTheme {
        OperationToolbar(
            matrices = emptyList(),
            selectedIndex = -1,
            secondaryIndex = -1,
            onSelectSecondary = {},
            onOperation = {}
        )
    }
}
