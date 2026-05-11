package com.cloveriris.calcore.presentation.visualization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.AnimationEvent
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.MemoryOpType
import com.cloveriris.calcore.domain.model.VisualizationLevel
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
class VisualizationViewModel @Inject constructor() : ViewModel() {

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

        _uiState.update {
            it.copy(
                bitGridBits = resultBits,
                bitGridLabel = "RESULT = %.6f".format(result),
                registers = registers,
                lastEventDescription = "计算结果: $result"
            )
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _uiState.update { state ->
                val regs = state.registers.toMutableList()
                if (regs.isNotEmpty()) {
                    regs[0] = regs[0].copy(isHighlighted = false)
                }
                state.copy(registers = regs)
            }
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
    val lastEventDescription: String = ""
)
