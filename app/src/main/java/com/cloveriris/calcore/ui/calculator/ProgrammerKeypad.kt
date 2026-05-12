package com.cloveriris.calcore.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.domain.model.BitWidth
import com.cloveriris.calcore.domain.model.CalculatorInput
import com.cloveriris.calcore.domain.model.NumberBase
import com.cloveriris.calcore.ui.components.ButtonType
import com.cloveriris.calcore.ui.components.CalcoreButton
import com.cloveriris.calcore.ui.components.ScrollableKeypadContainer

@Composable
fun ProgrammerKeypad(
    currentBase: NumberBase,
    currentBitWidth: BitWidth,
    onInput: (CalculatorInput) -> Unit,
    onBaseChange: (NumberBase) -> Unit,
    onBitWidthChange: (BitWidth) -> Unit,
    modifier: Modifier = Modifier,
    hasMemory: Boolean = false
) {
    ScrollableKeypadContainer(modifier = modifier) {
        val rowHeight = 52.dp

        // 进制切换行
        Row(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumberBase.entries.forEach { base ->
                val selected = base == currentBase
                CalcoreButton(
                    label = base.displayName,
                    onClick = { onBaseChange(base) },
                    modifier = Modifier.weight(1f),
                    type = if (selected) ButtonType.OPERATOR else ButtonType.FUNCTION
                )
            }
        }

        // 位宽切换行
        Row(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BitWidth.entries.forEach { width ->
                val selected = width == currentBitWidth
                CalcoreButton(
                    label = width.displayName,
                    onClick = { onBitWidthChange(width) },
                    modifier = Modifier.weight(1f),
                    type = if (selected) ButtonType.OPERATOR else ButtonType.FUNCTION
                )
            }
        }

        // 位运算行 1
        Row(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("AND", "OR", "XOR", "NOT", "Lsh", "Rsh").forEach { label ->
                CalcoreButton(
                    label = label,
                    onClick = { onInput(CalculatorInput.Operator(label)) },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.FUNCTION
                )
            }
        }

        // 算术运算行
        Row(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("+", "-", "×", "÷", "%").forEach { label ->
                CalcoreButton(
                    label = label,
                    onClick = { onInput(CalculatorInput.Operator(label)) },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.OPERATOR
                )
            }
        }

        // 功能行
        Row(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CalcoreButton(
                label = "CE",
                onClick = { onInput(CalculatorInput.ClearEntry) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
            CalcoreButton(
                label = "C",
                onClick = { onInput(CalculatorInput.Clear) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
            CalcoreButton(
                label = "⌫",
                onClick = { onInput(CalculatorInput.Backspace) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
            CalcoreButton(
                label = "=",
                onClick = { onInput(CalculatorInput.Equals) },
                modifier = Modifier.weight(1f),
                type = ButtonType.EQUALS
            )
        }

        // 数字键行（根据进制限制）
        val digitRows = getDigitRows(currentBase)
        digitRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { label ->
                    CalcoreButton(
                        label = label,
                        onClick = { onInput(CalculatorInput.Digit(label)) },
                        modifier = Modifier.weight(1f),
                        type = ButtonType.NUMBER
                    )
                }
            }
        }
    }
}

private fun getDigitRows(base: NumberBase): List<List<String>> {
    return when (base) {
        NumberBase.BIN -> listOf(
            listOf("0", "1")
        )
        NumberBase.OCT -> listOf(
            listOf("7", "6", "5"),
            listOf("4", "3", "2"),
            listOf("1", "0")
        )
        NumberBase.DEC -> listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("0")
        )
        NumberBase.HEX -> listOf(
            listOf("D", "E", "F"),
            listOf("A", "B", "C"),
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("0")
        )
    }
}
