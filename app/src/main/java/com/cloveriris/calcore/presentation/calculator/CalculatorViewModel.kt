package com.cloveriris.calcore.presentation.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.AnimationEvent
import com.cloveriris.calcore.domain.model.BitWidth
import com.cloveriris.calcore.domain.model.CalculatorInput
import com.cloveriris.calcore.domain.model.CalculatorMode
import com.cloveriris.calcore.domain.model.CalculatorState
import com.cloveriris.calcore.domain.model.HistoryEntry
import com.cloveriris.calcore.domain.model.MemoryOpType
import com.cloveriris.calcore.domain.model.MemoryState
import com.cloveriris.calcore.domain.model.NumberBase
import com.cloveriris.calcore.domain.model.SidePanelTab
import com.cloveriris.calcore.domain.usecase.EvaluateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val evaluateUseCase: EvaluateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _animationEvents = MutableSharedFlow<AnimationEvent>(extraBufferCapacity = 10)
    val animationEvents: SharedFlow<AnimationEvent> = _animationEvents.asSharedFlow()

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
        _uiState.update { it.copy(mode = mode, activeTab = SidePanelTab.NONE) }
    }

    fun onBaseChange(base: NumberBase) {
        _uiState.update { it.copy(numberBase = base) }
    }

    fun onBitWidthChange(width: BitWidth) {
        _uiState.update { it.copy(bitWidth = width) }
    }

    fun onProgrammerDigit(digit: String) {
        _uiState.update { current ->
            val base = current.numberBase
            val radix = base.radix
            val newValue = try {
                val digitValue = digit.toInt(radix)
                current.programmerValue * radix + digitValue
            } catch (_: Exception) {
                current.programmerValue
            }
            val truncated = current.bitWidth.truncate(newValue)
            current.copy(programmerValue = truncated)
        }
    }

    fun onProgrammerOperation(op: String) {
        _uiState.update { current ->
            val value = current.programmerValue
            val width = current.bitWidth
            val result = when (op) {
                "NOT" -> width.truncate(value.inv())
                "AND" -> value // 简化：二元运算需要操作数栈
                "OR" -> value
                "XOR" -> value
                "Lsh" -> width.truncate(value shl 1)
                "Rsh" -> width.truncate(value shr 1)
                else -> value
            }
            current.copy(programmerValue = result)
        }
    }

    fun onProgrammerClear() {
        _uiState.update { it.copy(programmerValue = 0L) }
    }

    fun onTabChange(tab: SidePanelTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun dismissPanel() {
        _uiState.update { it.copy(activeTab = SidePanelTab.NONE) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun recallHistory(entry: HistoryEntry) {
        _uiState.update {
            it.copy(
                state = CalculatorState.Inputting(entry.result),
                activeTab = SidePanelTab.NONE
            )
        }
    }

    private fun onDigit(digit: String) {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Idle -> {
                    _animationEvents.tryEmit(AnimationEvent.DigitEntered(digit.first(), digit))
                    current.copy(state = CalculatorState.Inputting(digit))
                }
                is CalculatorState.Inputting -> {
                    val newExpr = if (state.displayExpression == "0") {
                        digit
                    } else {
                        state.displayExpression + digit
                    }
                    _animationEvents.tryEmit(AnimationEvent.DigitEntered(digit.first(), newExpr))
                    current.copy(state = state.copy(displayExpression = newExpr))
                }
                is CalculatorState.Evaluated -> {
                    _animationEvents.tryEmit(AnimationEvent.DigitEntered(digit.first(), digit))
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
                        _animationEvents.tryEmit(AnimationEvent.OperatorEntered(op, op))
                        current.copy(state = CalculatorState.Inputting(op))
                    } else {
                        current
                    }
                }
                is CalculatorState.Inputting -> {
                    val expr = state.displayExpression
                    val newExpr = if (expr.isNotEmpty() && expr.last().isOperator()) {
                        expr.dropLast(1) + op
                    } else {
                        expr + op
                    }
                    _animationEvents.tryEmit(AnimationEvent.OperatorEntered(op, newExpr))
                    current.copy(state = state.copy(displayExpression = newExpr))
                }
                is CalculatorState.Evaluated -> {
                    val newExpr = state.displayResult + op
                    _animationEvents.tryEmit(AnimationEvent.OperatorEntered(op, newExpr))
                    current.copy(
                        state = CalculatorState.Inputting(displayExpression = newExpr)
                    )
                }
            }
        }
    }

    private fun onDecimal() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Idle -> {
                    _animationEvents.tryEmit(AnimationEvent.DecimalEntered)
                    current.copy(state = CalculatorState.Inputting("0."))
                }
                is CalculatorState.Inputting -> {
                    val parts = state.displayExpression.split(Regex("[+\\-×÷]"))
                    val lastNumber = parts.last()
                    if (!lastNumber.contains(".")) {
                        _animationEvents.tryEmit(AnimationEvent.DecimalEntered)
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
                    _animationEvents.tryEmit(AnimationEvent.DecimalEntered)
                    current.copy(state = CalculatorState.Inputting("0."))
                }
            }
        }
    }

    private fun onEquals() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Inputting -> {
                    val result = evaluateUseCase.evaluate(state.displayExpression)
                    val resultStr = result.fold(
                        onSuccess = ::formatResult,
                        onFailure = { "Error" }
                    )
                    val numericResult = result.getOrNull() ?: 0.0
                    _animationEvents.tryEmit(
                        AnimationEvent.Evaluated(state.displayExpression, numericResult)
                    )
                    val newHistory = if (result.isSuccess) {
                        listOf(
                            HistoryEntry(
                                expression = state.displayExpression,
                                result = resultStr
                            )
                        ) + current.history
                    } else current.history
                    current.copy(
                        state = CalculatorState.Evaluated(
                            displayExpression = state.displayExpression,
                            displayResult = resultStr
                        ),
                        history = newHistory.take(50)
                    )
                }
                else -> current
            }
        }
    }

    private fun onClear() {
        _animationEvents.tryEmit(AnimationEvent.Clear)
        _uiState.update { it.copy(state = CalculatorState.Idle()) }
    }

    private fun onClearEntry() {
        _animationEvents.tryEmit(AnimationEvent.Clear)
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
                    _animationEvents.tryEmit(AnimationEvent.Backspace)
                    val newExpr = state.displayExpression.dropLast(1)
                    if (newExpr.isEmpty() || newExpr == "-") {
                        current.copy(state = CalculatorState.Idle())
                    } else {
                        current.copy(state = state.copy(displayExpression = newExpr))
                    }
                }
                is CalculatorState.Evaluated -> {
                    _animationEvents.tryEmit(AnimationEvent.Backspace)
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
                    val result = evaluateUseCase.evaluate("${state.displayExpression} / 100")
                    val resultStr = result.fold(
                        onSuccess = ::formatResult,
                        onFailure = { "Error" }
                    )
                    current.copy(
                        state = CalculatorState.Evaluated(
                            displayExpression = state.displayExpression,
                            displayResult = resultStr
                        )
                    )
                }
                else -> current
            }
        }
    }

    private fun onReciprocal() {
        _uiState.update { current ->
            val expr = getCurrentExpression(current.state)
            val result = evaluateUseCase.evaluate("1 / ($expr)")
            val resultStr = result.fold(
                onSuccess = ::formatResult,
                onFailure = { "Error" }
            )
            current.copy(
                state = CalculatorState.Evaluated(
                    displayExpression = "1/($expr)",
                    displayResult = resultStr
                )
            )
        }
    }

    private fun onSquare() {
        _uiState.update { current ->
            val expr = getCurrentExpression(current.state)
            val result = evaluateUseCase.evaluate("($expr) ^ 2")
            val resultStr = result.fold(
                onSuccess = ::formatResult,
                onFailure = { "Error" }
            )
            current.copy(
                state = CalculatorState.Evaluated(
                    displayExpression = "sqr($expr)",
                    displayResult = resultStr
                )
            )
        }
    }

    private fun onSquareRoot() {
        _uiState.update { current ->
            val expr = getCurrentExpression(current.state)
            val result = evaluateUseCase.evaluate("sqrt($expr)")
            val resultStr = result.fold(
                onSuccess = ::formatResult,
                onFailure = { "Error" }
            )
            current.copy(
                state = CalculatorState.Evaluated(
                    displayExpression = "√($expr)",
                    displayResult = resultStr
                )
            )
        }
    }

    private fun onNegate() {
        _uiState.update { current ->
            when (val state = current.state) {
                is CalculatorState.Inputting -> {
                    val expr = state.displayExpression
                    val result = evaluateUseCase.evaluate("-($expr)")
                    val resultStr = result.fold(
                        onSuccess = ::formatResult,
                        onFailure = { "Error" }
                    )
                    current.copy(
                        state = CalculatorState.Inputting(
                            displayExpression = resultStr
                        )
                    )
                }
                is CalculatorState.Evaluated -> {
                    val result = evaluateUseCase.evaluate("-(${state.displayResult})")
                    val resultStr = result.fold(
                        onSuccess = ::formatResult,
                        onFailure = { "Error" }
                    )
                    current.copy(
                        state = CalculatorState.Inputting(
                            displayExpression = resultStr
                        )
                    )
                }
                else -> current
            }
        }
    }

    private fun onMemoryClear() {
        _animationEvents.tryEmit(AnimationEvent.MemoryOperation(MemoryOpType.CLEAR))
        _uiState.update { it.copy(memory = MemoryState()) }
    }

    private fun onMemoryRecall() {
        _animationEvents.tryEmit(AnimationEvent.MemoryOperation(MemoryOpType.RECALL))
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
            val value = getCurrentValue(current.state)
            _animationEvents.tryEmit(AnimationEvent.MemoryOperation(MemoryOpType.STORE))
            current.copy(memory = MemoryState(value = value, hasValue = true))
        }
    }

    private fun onMemoryAdd() {
        _uiState.update { current ->
            val value = getCurrentValue(current.state)
            val newValue = if (current.memory.hasValue) current.memory.value + value else value
            _animationEvents.tryEmit(AnimationEvent.MemoryOperation(MemoryOpType.ADD))
            current.copy(memory = MemoryState(value = newValue, hasValue = true))
        }
    }

    private fun onMemorySubtract() {
        _uiState.update { current ->
            val value = getCurrentValue(current.state)
            val newValue = if (current.memory.hasValue) current.memory.value - value else -value
            _animationEvents.tryEmit(AnimationEvent.MemoryOperation(MemoryOpType.SUBTRACT))
            current.copy(memory = MemoryState(value = newValue, hasValue = true))
        }
    }

    private fun getCurrentExpression(state: CalculatorState): String {
        return when (state) {
            is CalculatorState.Idle -> "0"
            is CalculatorState.Inputting -> state.displayExpression
            is CalculatorState.Evaluated -> state.displayResult
        }
    }

    private fun getCurrentValue(state: CalculatorState): Double {
        return when (state) {
            is CalculatorState.Idle -> 0.0
            is CalculatorState.Inputting -> {
                evaluateUseCase.evaluate(state.displayExpression)
                    .getOrNull() ?: 0.0
            }
            is CalculatorState.Evaluated -> {
                state.displayResult.toDoubleOrNull() ?: 0.0
            }
        }
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
    val memory: MemoryState = MemoryState(),
    val history: List<HistoryEntry> = emptyList(),
    val activeTab: SidePanelTab = SidePanelTab.NONE,
    val numberBase: NumberBase = NumberBase.DEC,
    val bitWidth: BitWidth = BitWidth.QWORD,
    val programmerValue: Long = 0L
)
