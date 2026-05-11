package com.cloveriris.calcore.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
fun ScientificKeypad(
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
        // 角度/辅助行
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("DEG", "HYP", "F-E", "π", "e").forEach { label ->
                val input = when (label) {
                    "π" -> CalculatorInput.Digit("π")
                    "e" -> CalculatorInput.Digit("e")
                    else -> CalculatorInput.Digit(label)
                }
                CalcoreButton(
                    label = label,
                    onClick = { onInput(input) },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.FUNCTION
                )
            }
        }

        // 内存行
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
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

        // 科学函数行 1
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("x²", "xʸ", "sin", "cos", "tan").forEach { label ->
                val input = when (label) {
                    "x²" -> CalculatorInput.Square
                    "xʸ" -> CalculatorInput.Operator("^")
                    "sin" -> CalculatorInput.Operator("sin(")
                    "cos" -> CalculatorInput.Operator("cos(")
                    "tan" -> CalculatorInput.Operator("tan(")
                    else -> CalculatorInput.Digit(label)
                }
                CalcoreButton(
                    label = label,
                    onClick = { onInput(input) },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.FUNCTION
                )
            }
        }

        // 科学函数行 2
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("x³", "³√x", "sin⁻¹", "cos⁻¹", "tan⁻¹").forEach { label ->
                CalcoreButton(
                    label = label,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.FUNCTION
                )
            }
        }

        // 科学函数行 3
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("√", "10ˣ", "log", "Exp", "Mod").forEach { label ->
                val input = when (label) {
                    "√" -> CalculatorInput.SquareRoot
                    "log" -> CalculatorInput.Operator("log(")
                    "Mod" -> CalculatorInput.Operator("%")
                    else -> CalculatorInput.Digit(label)
                }
                CalcoreButton(
                    label = label,
                    onClick = { onInput(input) },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.FUNCTION
                )
            }
        }

        // 科学函数行 4
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("¹/x", "eˣ", "ln", "dms", "deg").forEach { label ->
                val input = when (label) {
                    "¹/x" -> CalculatorInput.Reciprocal
                    "ln" -> CalculatorInput.Operator("ln(")
                    else -> CalculatorInput.Digit(label)
                }
                CalcoreButton(
                    label = label,
                    onClick = { onInput(input) },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.FUNCTION
                )
            }
        }

        // 标准功能行
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
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
                label = "÷",
                onClick = { onInput(CalculatorInput.Operator("÷")) },
                modifier = Modifier.weight(1f),
                type = ButtonType.OPERATOR
            )
        }

        // 数字行
        val numbers = listOf(
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+")
        )
        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
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

        // 底部行
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
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
