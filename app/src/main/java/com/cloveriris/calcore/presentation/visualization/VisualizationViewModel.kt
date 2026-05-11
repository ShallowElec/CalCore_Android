package com.cloveriris.calcore.presentation.visualization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.AnimationEvent
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.MemoryOpType
import com.cloveriris.calcore.domain.model.VisualizationLevel
import com.cloveriris.calcore.domain.usecase.EvaluateUseCase
import com.cloveriris.calcore.engine.parser.Expression
import com.cloveriris.calcore.ui.visualization.MemoryCellVisual
import com.cloveriris.calcore.ui.visualization.RegisterVisual
import com.cloveriris.calcore.ui.visualization.StackFrameVisual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 可视化舞台 ViewModel
 *
 * 接收 AnimationEvent，生成对应的视觉状态，驱动 Canvas 组件。
 */
@HiltViewModel
class VisualizationViewModel @Inject constructor(
    private val evaluateUseCase: EvaluateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisualizationUiState())
    val uiState: StateFlow<VisualizationUiState> = _uiState.asStateFlow()

    fun onEvent(event: AnimationEvent) {
        when (event) {
            is AnimationEvent.DigitEntered -> handleDigit(event.digit, event.currentExpression)
            is AnimationEvent.OperatorEntered -> handleOperator(event.operator, event.currentExpression)
            is AnimationEvent.Evaluated -> handleEvaluated(event.expression, event.result)
            AnimationEvent.Clear -> handleClear()
            AnimationEvent.Backspace -> handleBackspace()
            is AnimationEvent.MemoryOperation -> handleMemory(event.type)
            AnimationEvent.DecimalEntered -> handleDecimal()
            is AnimationEvent.ExpressionParsed -> handleExpressionParsed(event.expression)
            is AnimationEvent.BitOperation -> handleBitOperation(event)
        }
    }

    fun setEvaluationProgress(progress: Float) {
        _uiState.update { it.copy(evaluationProgress = progress.coerceIn(0f, 1f)) }
    }

    fun playPauseEvaluation() {
        val current = _uiState.value.evaluationProgress
        if (current >= 0.99f) {
            // 如果已完成，重新开始
            restartEvaluation()
        } else {
            // 切换播放/暂停状态（通过检查是否在动画中）
            _uiState.update { it.copy(evaluationProgress = if (current > 0f) current else 0.01f) }
        }
    }

    fun restartEvaluation() {
        _uiState.update { it.copy(evaluationProgress = 0f) }
        // 重新触发自动播放
        viewModelScope.launch {
            val steps = 20
            repeat(steps) { i ->
                kotlinx.coroutines.delay(80)
                _uiState.update { state ->
                    state.copy(evaluationProgress = (i + 1) / steps.toFloat())
                }
            }
        }
    }

    fun setArchitecture(arch: Architecture) {
        _uiState.update { it.copy(architecture = arch) }
    }

    fun setPanelExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isPanelExpanded = expanded) }
    }

    fun toggleLevel(level: VisualizationLevel) {
        _uiState.update { current ->
            val newLevels = if (level in current.activeLevels) {
                current.activeLevels - level
            } else {
                current.activeLevels + level
            }
            current.copy(activeLevels = newLevels)
        }
    }

    // ==================== 事件处理 ====================

    private fun handleDigit(digit: Char, currentExpression: String) {
        val ascii = digit.code
        val bits = List(64) { i ->
            if (i < 8) (ascii shr i) and 1 == 1 else false
        }

        // 将当前表达式解析为数值（仅用于演示）
        val numericValue = currentExpression.toDoubleOrNull() ?: 0.0

        val registers = _uiState.value.registers.toMutableList()
        if (registers.isEmpty()) {
            // 初始化寄存器
            val names = _uiState.value.architecture.registerNames
            registers.addAll(names.map { RegisterVisual(it, 0L) })
        }
        // RAX/X0/A0 显示当前数值
        if (registers.isNotEmpty()) {
            registers[0] = registers[0].copy(
                value = numericValue.toLong(),
                isHighlighted = true
            )
        }

        _uiState.update {
            it.copy(
                bitGridBits = bits,
                bitGridLabel = "ASCII '${digit}' = 0x%02X".format(ascii),
                registers = registers,
                lastEventDescription = "输入数字: $digit"
            )
        }

        // 300ms 后取消高亮
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _uiState.update { state ->
                val regs = state.registers.toMutableList()
                if (regs.isNotEmpty()) {
                    regs[0] = regs[0].copy(isHighlighted = false)
                }
                state.copy(registers = regs)
            }
        }
    }

    private fun handleOperator(op: String, currentExpression: String) {
        _uiState.update {
            it.copy(
                bitGridLabel = "OPERATOR: $op",
                lastEventDescription = "输入运算符: $op"
            )
        }
    }

    private fun handleEvaluated(expression: String, result: Double) {
        val resultBits = List(64) { i ->
            if (i < 64) (result.toRawBits() shr i) and 1 == 1L else false
        }

        val registers = _uiState.value.registers.toMutableList()
        if (registers.isEmpty()) {
            val names = _uiState.value.architecture.registerNames
            registers.addAll(names.map { RegisterVisual(it, 0L) })
        }
        if (registers.isNotEmpty()) {
            registers[0] = registers[0].copy(
                value = result.toLong(),
                isHighlighted = true
            )
        }

        // 解析 AST 用于可视化
        val ast = evaluateUseCase.parse(expression).getOrNull()

        _uiState.update {
            it.copy(
                bitGridBits = resultBits,
                bitGridLabel = "RESULT = %.6f".format(result),
                registers = registers,
                currentAst = ast,
                evaluationProgress = 0f,
                lastEventDescription = "计算结果: $result"
            )
        }

        // 自动播放求值进度动画
        viewModelScope.launch {
            val steps = 20
            repeat(steps) { i ->
                kotlinx.coroutines.delay(80)
                _uiState.update { state ->
                    state.copy(evaluationProgress = (i + 1) / steps.toFloat())
                }
            }
            kotlinx.coroutines.delay(200)
            _uiState.update { state ->
                val regs = state.registers.toMutableList()
                if (regs.isNotEmpty()) {
                    regs[0] = regs[0].copy(isHighlighted = false)
                }
                state.copy(registers = regs)
            }
        }
    }

    private fun handleExpressionParsed(expression: String) {
        val ast = evaluateUseCase.parse(expression).getOrNull()
        _uiState.update {
            it.copy(
                currentAst = ast,
                evaluationProgress = 0f,
                lastEventDescription = "解析表达式"
            )
        }
    }

    private fun handleBitOperation(event: AnimationEvent.BitOperation) {
        val leftBits = List(64) { i -> ((event.left shr i) and 1L) == 1L }
        val rightBits = List(64) { i -> ((event.right shr i) and 1L) == 1L }
        val resultBits = List(64) { i -> ((event.result shr i) and 1L) == 1L }

        _uiState.update {
            it.copy(
                bitGridBits = resultBits,
                bitGridLabel = "${event.op}: 0x${event.left.toString(16).uppercase()} ${event.op} 0x${event.right.toString(16).uppercase()}",
                lastEventDescription = "位运算: ${event.op}",
                bitOperationLeft = leftBits,
                bitOperationRight = rightBits
            )
        }
    }

    private fun handleClear() {
        _uiState.update {
            it.copy(
                bitGridBits = List(64) { false },
                bitGridLabel = "CLEARED",
                registers = emptyList(),
                lastEventDescription = "清除"
            )
        }
    }

    private fun handleBackspace() {
        _uiState.update {
            it.copy(lastEventDescription = "退格")
        }
    }

    private fun handleMemory(type: MemoryOpType) {
        val desc = when (type) {
            MemoryOpType.STORE -> "内存存储"
            MemoryOpType.RECALL -> "内存读取"
            MemoryOpType.ADD -> "内存加"
            MemoryOpType.SUBTRACT -> "内存减"
            MemoryOpType.CLEAR -> "内存清除"
        }
        _uiState.update { it.copy(lastEventDescription = desc) }
    }

    private fun handleDecimal() {
        _uiState.update { it.copy(lastEventDescription = "小数点") }
    }
}

/**
 * 可视化 UI 状态
 */
data class VisualizationUiState(
    val architecture: Architecture = Architecture.X86_64,
    val bitGridBits: List<Boolean> = List(64) { false },
    val bitGridLabel: String = "IDLE",
    val registers: List<RegisterVisual> = emptyList(),
    val memoryCells: List<MemoryCellVisual> = emptyList(),
    val stackFrames: List<StackFrameVisual> = emptyList(),
    val activeLevels: Set<VisualizationLevel> = VisualizationLevel.entries.toSet(),
    val isPanelExpanded: Boolean = false,
    val lastEventDescription: String = "",
    val currentAst: Expression? = null,
    val evaluationProgress: Float = 0f,
    val bitOperationLeft: List<Boolean> = emptyList(),
    val bitOperationRight: List<Boolean> = emptyList()
)
