package com.cloveriris.calcore.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.domain.model.CalculatorInput
import com.cloveriris.calcore.ui.components.ButtonType
import com.cloveriris.calcore.ui.components.CalcoreButton

@Composable
fun StandardKeypad(
    onInput: (CalculatorInput) -> Unit,
    modifier: Modifier = Modifier,
    hasMemory: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Memory row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CalcoreButton(
                label = "MC",
                onClick = { onInput(CalculatorInput.MemoryClear) },
                modifier = Modifier.weight(1f),
                type = ButtonType.MEMORY
            )
            CalcoreButton(
                label = "MR",
                onClick = { onInput(CalculatorInput.MemoryRecall) },
                modifier = Modifier.weight(1f),
                type = ButtonType.MEMORY
            )
            CalcoreButton(
                label = "M+",
                onClick = { onInput(CalculatorInput.MemoryAdd) },
                modifier = Modifier.weight(1f),
                type = ButtonType.MEMORY
            )
            CalcoreButton(
                label = "M-",
                onClick = { onInput(CalculatorInput.MemorySubtract) },
                modifier = Modifier.weight(1f),
                type = ButtonType.MEMORY
            )
            CalcoreButton(
                label = "MS",
                onClick = { onInput(CalculatorInput.MemoryStore) },
                modifier = Modifier.weight(1f),
                type = ButtonType.MEMORY
            )
        }

        // Function row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CalcoreButton(
                label = "%",
                onClick = { onInput(CalculatorInput.Percent) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
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
        }

        // Function row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CalcoreButton(
                label = "¹/ₓ",
                onClick = { onInput(CalculatorInput.Reciprocal) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
            CalcoreButton(
                label = "x²",
                onClick = { onInput(CalculatorInput.Square) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
            CalcoreButton(
                label = "²√x",
                onClick = { onInput(CalculatorInput.SquareRoot) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
            CalcoreButton(
                label = "÷",
                onClick = { onInput(CalculatorInput.Operator("÷")) },
                modifier = Modifier.weight(1f),
                type = ButtonType.OPERATOR
            )
        }

        // Number rows
        val numbers = listOf(
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+")
        )

        numbers.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { label ->
                    val type = if (label in listOf("×", "-", "+")) {
                        ButtonType.OPERATOR
                    } else {
                        ButtonType.NUMBER
                    }
                    val input = if (type == ButtonType.OPERATOR) {
                        CalculatorInput.Operator(label)
                    } else {
                        CalculatorInput.Digit(label)
                    }
                    CalcoreButton(
                        label = label,
                        onClick = { onInput(input) },
                        modifier = Modifier.weight(1f),
                        type = type
                    )
                }
            }
        }

        // Bottom row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CalcoreButton(
                label = "±",
                onClick = { onInput(CalculatorInput.Negate) },
                modifier = Modifier.weight(1f),
                type = ButtonType.FUNCTION
            )
            CalcoreButton(
                label = "0",
                onClick = { onInput(CalculatorInput.Digit("0")) },
                modifier = Modifier.weight(1f),
                type = ButtonType.NUMBER
            )
            CalcoreButton(
                label = ".",
                onClick = { onInput(CalculatorInput.Decimal) },
                modifier = Modifier.weight(1f),
                type = ButtonType.NUMBER
            )
            CalcoreButton(
                label = "=",
                onClick = { onInput(CalculatorInput.Equals) },
                modifier = Modifier.weight(1f),
                type = ButtonType.EQUALS
            )
        }
    }
}
