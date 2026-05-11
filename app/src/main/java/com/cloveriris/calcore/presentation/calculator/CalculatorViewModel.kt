package com.cloveriris.calcore.presentation.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.CalculatorInput
import com.cloveriris.calcore.domain.model.CalculatorMode
import com.cloveriris.calcore.domain.model.CalculatorState
import com.cloveriris.calcore.domain.model.MemoryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

@HiltViewModel
class CalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun onInput(input: CalculatorInput) {
        when (input) {
            is CalculatorInput.Digit -> onDigit(input.value)
            is CalculatorInput.Operator -> onOperator(input.op)
            CalculatorInput.Decimal -> onDecimal()
            CalculatorInput.Equals -> onEquals()
            CalculatorInput.Clear -> onClear()
            CalculatorInput.ClearEntry -> onClearEntry()
            CalculatorInput.Backspace -> onBackspace()
            CalculatorInput.Percent -> onPercent()
            CalculatorInput.Reciprocal -> onReciprocal()
            CalculatorInput.Square -> onSquare()
            CalculatorInput.SquareRoot -> onSquareRoot()
            CalculatorInput.Negate -> onNegate()
            CalculatorInput.MemoryClear -> onMemoryClear()
            CalculatorInput.MemoryRecall -> onMemoryRecall()
            CalculatorInput.MemoryStore -> onMemoryStore()
            CalculatorInput.MemoryAdd -> onMemoryAdd()
            CalculatorInput.MemorySubtract -> onMemorySubtract()
        }
    }

    fun onModeChange(mode: CalculatorMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    private fun onDigit(digit: String) {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Idle -> {
                    current.copy(state = CalculatorState.Inputting(digit))
                }
                is CalculatorState.Inputting -> {
                    val newExpr = if (state.displayExpression == "0") {
                        digit
                    } else {
                        state.displayExpression + digit
                    }
                    current.copy(state = state.copy(displayExpression = newExpr))
                }
                is CalculatorState.Evaluated -> {
                    current.copy(state = CalculatorState.Inputting(digit))
                }
            }
        }
    }

    private fun onOperator(op: String) {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Idle -> {
                    if (op == "-") {
                        current.copy(state = CalculatorState.Inputting(op))
                    } else {
                        current
                    }
                }
                is CalculatorState.Inputting -> {
                    val expr = state.displayExpression
                    val newExpr = if (expr.last().isOperator()) {
                        expr.dropLast(1) + op
                    } else {
                        expr + op
                    }
                    current.copy(state = state.copy(displayExpression = newExpr))
                }
                is CalculatorState.Evaluated -> {
                    current.copy(
                        state = CalculatorState.Inputting(
                            displayExpression = state.displayResult + op
                        )
                    )
                }
            }
        }
    }

    private fun onDecimal() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Idle -> {
                    current.copy(state = CalculatorState.Inputting("0."))
                }
                is CalculatorState.Inputting -> {
                    val parts = state.displayExpression.split(Regex("[+\\-×÷]"))
                    val lastNumber = parts.last()
                    if (!lastNumber.contains(".")) {
                        current.copy(
                            state = state.copy(
                                displayExpression = state.displayExpression + "."
                            )
                        )
                    } else {
                        current
                    }
                }
                is CalculatorState.Evaluated -> {
                    current.copy(state = CalculatorState.Inputting("0."))
                }
            }
        }
    }

    private fun onEquals() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Inputting -> {
                    val result = evaluateExpression(state.displayExpression)
                    current.copy(
                        state = CalculatorState.Evaluated(
                            displayExpression = state.displayExpression,
                            displayResult = formatResult(result)
                        )
                    )
                }
                else -> current
            }
        }
    }

    private fun onClear() {
        _uiState.update { it.copy(state = CalculatorState.Idle()) }
    }

    private fun onClearEntry() {
        _uiState.update { current ->
            when (current.state) {
                is CalculatorState.Inputting -> {
                    current.copy(state = CalculatorState.Idle())
                }
                else -> current
            }
        }
    }

    private fun onBackspace() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Inputting -> {
                    val newExpr = state.displayExpression.dropLast(1)
                    if (newExpr.isEmpty() || newExpr == "-") {
                        current.copy(state = CalculatorState.Idle())
                    } else {
                        current.copy(state = state.copy(displayExpression = newExpr))
                    }
                }
                is CalculatorState.Evaluated -> {
                    current.copy(state = CalculatorState.Idle())
                }
                else -> current
            }
        }
    }

    private fun onPercent() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Inputting -> {
                    val value = state.displayExpression.toDoubleOrNull() ?: 0.0
                    val result = value / 100.0
                    current.copy(
                        state = CalculatorState.Evaluated(
                            displayExpression = state.displayExpression,
                            displayResult = formatResult(result)
                        )
                    )
                }
                else -> current
            }
        }
    }

    private fun onReciprocal() {
        _uiState.update { current ->
            val value = getCurrentNumericValue(current.state)
            val result = if (value != 0.0) 1.0 / value else Double.NaN
            current.copy(
                state = CalculatorState.Evaluated(
                    displayExpression = "1/(${getDisplayExpression(current.state)})",
                    displayResult = formatResult(result)
                )
            )
        }
    }

    private fun onSquare() {
        _uiState.update { current ->
            val value = getCurrentNumericValue(current.state)
            val result = value.pow(2)
            current.copy(
                state = CalculatorState.Evaluated(
                    displayExpression = "sqr(${getDisplayExpression(current.state)})",
                    displayResult = formatResult(result)
                )
            )
        }
    }

    private fun onSquareRoot() {
        _uiState.update { current ->
            val value = getCurrentNumericValue(current.state)
            val result = sqrt(value)
            current.copy(
                state = CalculatorState.Evaluated(
                    displayExpression = "√(${getDisplayExpression(current.state)})",
                    displayResult = formatResult(result)
                )
            )
        }
    }

    private fun onNegate() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Inputting -> {
                    val expr = state.displayExpression
                    val newExpr = if (expr.startsWith("-")) expr.drop(1) else "-$expr"
                    current.copy(state = state.copy(displayExpression = newExpr))
                }
                is CalculatorState.Evaluated -> {
                    val value = state.displayResult.toDoubleOrNull() ?: 0.0
                    current.copy(
                        state = CalculatorState.Inputting(
                            displayExpression = formatResult(-value)
                        )
                    )
                }
                else -> current
            }
        }
    }

    private fun onMemoryClear() {
        _uiState.update { it.copy(memory = MemoryState()) }
    }

    private fun onMemoryRecall() {
        _uiState.update { current ->
            if (current.memory.hasValue) {
                current.copy(
                    state = CalculatorState.Inputting(
                        displayExpression = formatResult(current.memory.value)
                    )
                )
            } else {
                current
            }
        }
    }

    private fun onMemoryStore() {
        _uiState.update { current ->
            val value = getCurrentNumericValue(current.state)
            current.copy(memory = MemoryState(value = value, hasValue = true))
        }
    }

    private fun onMemoryAdd() {
        _uiState.update { current ->
            val value = getCurrentNumericValue(current.state)
            val newValue = if (current.memory.hasValue) current.memory.value + value else value
            current.copy(memory = MemoryState(value = newValue, hasValue = true))
        }
    }

    private fun onMemorySubtract() {
        _uiState.update { current ->
            val value = getCurrentNumericValue(current.state)
            val newValue = if (current.memory.hasValue) current.memory.value - value else -value
            current.copy(memory = MemoryState(value = newValue, hasValue = true))
        }
    }

    private fun getCurrentNumericValue(state: CalculatorState): Double {
        return when (state) {
            is CalculatorState.Idle -> 0.0
            is CalculatorState.Inputting -> state.displayExpression.toDoubleOrNull() ?: 0.0
            is CalculatorState.Evaluated -> state.displayResult.toDoubleOrNull() ?: 0.0
        }
    }

    private fun getDisplayExpression(state: CalculatorState): String {
        return when (state) {
            is CalculatorState.Idle -> "0"
            is CalculatorState.Inputting -> state.displayExpression
            is CalculatorState.Evaluated -> state.displayResult
        }
    }

    private fun evaluateExpression(expression: String): Double {
        return try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
            evaluateSimpleExpression(sanitized)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun evaluateSimpleExpression(expr: String): Double {
        // Simple recursive descent parser for basic arithmetic
        val tokens = tokenize(expr)
        var index = 0

        fun parseFactor(): Double {
            val token = tokens[index++]
            return token.toDoubleOrNull() ?: 0.0
        }

        fun parseTerm(): Double {
            var value = parseFactor()
            while (index < tokens.size && (tokens[index] == "*" || tokens[index] == "/")) {
                val op = tokens[index++]
                val right = parseFactor()
                value = if (op == "*") value * right else value / right
            }
            return value
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (index < tokens.size && (tokens[index] == "+" || tokens[index] == "-")) {
                val op = tokens[index++]
                val right = parseTerm()
                value = if (op == "+") value + right else value - right
            }
            return value
        }

        return parseExpression()
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = ""
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    current += ch
                }
                ch == '+' || ch == '-' || ch == '*' || ch == '/' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current)
                        current = ""
                    }
                    // Handle negative numbers
                    if (ch == '-' && (i == 0 || expr[i - 1] in "+*/")) {
                        current += ch
                    } else {
                        tokens.add(ch.toString())
                    }
                }
                else -> {}
            }
            i++
        }
        if (current.isNotEmpty()) {
            tokens.add(current)
        }
        return tokens
    }

    private fun formatResult(value: Double): String {
        return when {
            value.isNaN() -> "Error"
            value.isInfinite() -> "Error"
            value == value.toLong().toDouble() -> value.toLong().toString()
            else -> value.toString().trimEnd('0').trimEnd('.')
        }
    }

    private fun Char.isOperator(): Boolean = this in "+-×÷"
}

/**
 * 计算器 UI 状态
 */
data class CalculatorUiState(
    val mode: CalculatorMode = CalculatorMode.STANDARD,
    val state: CalculatorState = CalculatorState.Idle(),
    val memory: MemoryState = MemoryState()
)
