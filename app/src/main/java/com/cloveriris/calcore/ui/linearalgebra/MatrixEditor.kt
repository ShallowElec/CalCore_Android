package com.cloveriris.calcore.ui.linearalgebra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 矩阵 Grid 编辑器
 *
 * 提供等宽高亮网格，支持单元格实时编辑、行/列增删。
 *
 * @param matrix 当前编辑的矩阵
 * @param onCellChange 单元格值变更回调
 * @param onAddRow 增加一行
 * @param onRemoveRow 减少一行
 * @param onAddCol 增加一列
 * @param onRemoveCol 减少一列
 */
@Composable
fun MatrixEditor(
    matrix: Matrix<Double>,
    onCellChange: (row: Int, col: Int, value: Double) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: () -> Unit,
    onAddCol: () -> Unit,
    onRemoveCol: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = matrix.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
        ) {
            repeat(matrix.rows) { row ->
                Row {
                    repeat(matrix.cols) { col ->
                        MatrixCell(
                            value = matrix[row, col],
                            row = row,
                            col = col,
                            onValueChange = onCellChange
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onAddRow,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("+行", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onRemoveRow,
                enabled = matrix.rows > 1,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("-行", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onAddCol,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("+列", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onRemoveCol,
                enabled = matrix.cols > 1,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("-列", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MatrixCell(
    value: Double,
    row: Int,
    col: Int,
    onValueChange: (row: Int, col: Int, value: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(formatDouble(value)) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            text = formatDouble(value)
        }
    }

    val isZero = value == 0.0
    val textColor = if (isZero && !isFocused) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 56.dp, minHeight = 48.dp)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor)
    ) {
        if (!isZero) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(TerminalGreen)
            )
        }

        BasicTextField(
            value = text,
            onValueChange = { newValue ->
                when {
                    newValue.isEmpty() -> {
                        text = ""
                        onValueChange(row, col, 0.0)
                    }

                    newValue == "-" || newValue == "." || newValue == "-." -> {
                        text = newValue
                    }

                    else -> {
                        val parsed = newValue.toDoubleOrNull()
                        if (parsed != null) {
                            text = newValue
                            onValueChange(row, col, parsed)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (!isZero) 8.dp else 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = textColor,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        ) { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    }
}

private fun formatDouble(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

@Preview(showBackground = true)
@Composable
private fun MatrixEditorPreview() {
    CalcoreTheme {
        MatrixEditor(
            matrix = Matrix(
                rows = 3,
                cols = 3,
                data = listOf(
                    listOf(1.0, 0.0, -3.5),
                    listOf(0.0, 2.0, 0.0),
                    listOf(4.0, 0.0, 1.0)
                ),
                name = "A"
            ),
            onCellChange = { _, _, _ -> },
            onAddRow = {},
            onRemoveRow = {},
            onAddCol = {},
            onRemoveCol = {}
        )
    }
}
