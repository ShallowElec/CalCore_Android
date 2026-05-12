package com.cloveriris.calcore.presentation.visualization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.domain.model.AnimationAction
import com.cloveriris.calcore.domain.model.AnimationEvent
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.VisualizationLevel
import com.cloveriris.calcore.engine.visualization.VisualizationEngine
import com.cloveriris.calcore.ui.visualization.MemoryCellVisual
import com.cloveriris.calcore.ui.visualization.RegisterVisual
import com.cloveriris.calcore.ui.visualization.StackFrameVisual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 可视化舞台 ViewModel
 *
 * 接收 AnimationEvent，通过 VisualizationEngine 生成 AnimationScript，
 * 由 ScriptPlayer 驱动按时间回放，最终输出 VisualizationUiState 供 Canvas 消费。
 */
@HiltViewModel
class VisualizationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(VisualizationUiState())
    val uiState: StateFlow<VisualizationUiState> = _uiState.asStateFlow()

    private var currentScript: com.cloveriris.calcore.domain.model.AnimationScript? = null
    private var playbackJob: Job? = null

    /** 基准 step 间隔（毫秒），默认 200ms */
    private val baseStepIntervalMs = 200L

    fun onEvent(event: AnimationEvent) {
        val arch = _uiState.value.architecture
        val script = VisualizationEngine.generateScript(event, arch)
        currentScript = script
        _uiState.update {
            // 保留用户设置和上次的内容描述，避免 placeholder → content 闪烁
            VisualizationUiState(
                architecture = it.architecture,
                playbackSpeed = it.playbackSpeed,
                activeLevels = it.activeLevels,
                isPanelExpanded = it.isPanelExpanded,
                lastEventDescription = it.lastEventDescription
            )
        }
        startPlayback(script)
    }

    fun setEvaluationProgress(progress: Float) {
        playbackJob?.cancel()
        currentScript?.let { applyScriptAtProgress(progress.coerceIn(0f, 1f), it) }
    }

    fun playPauseEvaluation() {
        val current = _uiState.value
        if (current.evaluationProgress >= 0.99f) {
            currentScript?.let { startPlayback(it) }
        } else if (current.isPlaying) {
            playbackJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            currentScript?.let { resumePlayback(it) }
        }
    }

    fun restartEvaluation() {
        currentScript?.let { startPlayback(it) }
    }

    fun setArchitecture(arch: Architecture) {
        _uiState.update { it.copy(architecture = arch) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed.coerceIn(0.2f, 2.0f)) }
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

    // ==================== 播放控制 ====================

    private fun startPlayback(script: com.cloveriris.calcore.domain.model.AnimationScript) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true, totalDurationMs = script.totalDurationMs) }
            val speed = _uiState.value.playbackSpeed
            val stepCount = (script.totalDurationMs / baseStepIntervalMs).toInt().coerceAtLeast(8)
            val stepDelay = (baseStepIntervalMs / speed).toLong().coerceAtLeast(50L)
            repeat(stepCount) { i ->
                delay(stepDelay)
                val progress = (i + 1) / stepCount.toFloat()
                applyScriptAtProgress(progress, script)
            }
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    evaluationProgress = 1f,
                    currentTimeMs = script.totalDurationMs
                )
            }
        }
    }

    private fun resumePlayback(script: com.cloveriris.calcore.domain.model.AnimationScript) {
        val currentProgress = _uiState.value.evaluationProgress
        playbackJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            val speed = _uiState.value.playbackSpeed
            val stepCount = (script.totalDurationMs / baseStepIntervalMs).toInt().coerceAtLeast(8)
            val stepDelay = (baseStepIntervalMs / speed).toLong().coerceAtLeast(50L)
            val remainingSteps = ((1f - currentProgress) * stepCount).toInt().coerceAtLeast(1)
            repeat(remainingSteps) { i ->
                delay(stepDelay)
                val progress = currentProgress + (i + 1) / stepCount.toFloat() * (1f - currentProgress)
                applyScriptAtProgress(progress, script)
            }
            _uiState.update { it.copy(isPlaying = false, evaluationProgress = 1f) }
        }
    }

    private fun applyScriptAtProgress(
        progress: Float,
        script: com.cloveriris.calcore.domain.model.AnimationScript
    ) {
        val timeMs = (progress * script.totalDurationMs).toLong()
        val actions = script.events.filter { it.timeMs <= timeMs }.map { it.action }
        var newState = _uiState.value.copy(
            evaluationProgress = progress,
            currentTimeMs = timeMs,
            totalDurationMs = script.totalDurationMs
        )
        for (action in actions) {
            newState = reduceAction(newState, action)
        }
        _uiState.value = newState.copy(isPlaying = _uiState.value.isPlaying)
    }

    // ==================== 状态归约器 ====================

    private fun reduceAction(state: VisualizationUiState, action: AnimationAction): VisualizationUiState {
        return when (action) {
            is AnimationAction.UpdateBitGrid ->
                state.copy(bitGridBits = action.bits, bitGridLabel = action.label)

            is AnimationAction.UpdateBitGridHighlights ->
                state.copy(bitGridHighlights = action.highlightIndices)

            is AnimationAction.UpdateRegister -> {
                val regs = state.registers.toMutableList()
                if (action.index < regs.size) {
                    regs[action.index] = regs[action.index].copy(
                        value = action.value,
                        isHighlighted = action.isHighlighted
                    )
                } else if (action.index == 0 && regs.isEmpty()) {
                    regs.addAll(state.architecture.registerNames.map { RegisterVisual(it, 0L) })
                    regs[0] = regs[0].copy(value = action.value, isHighlighted = action.isHighlighted)
                }
                state.copy(registers = regs)
            }

            is AnimationAction.WriteMemory -> {
                val cells = state.memoryCells.toMutableList()
                val addrStr = "0x%04X".format(action.address)
                val existing = cells.indexOfFirst { it.address == addrStr }
                val cell = MemoryCellVisual(
                    addrStr,
                    action.value,
                    action.isAllocated,
                    action.isPointer,
                    action.isWriting
                )
                if (existing >= 0) cells[existing] = cell else cells.add(cell)
                state.copy(memoryCells = cells)
            }

            is AnimationAction.PushStack -> {
                val frames = state.stackFrames + StackFrameVisual(action.label, action.address)
                state.copy(stackFrames = frames)
            }

            is AnimationAction.PopStack ->
                state.copy(stackFrames = state.stackFrames.dropLast(1))

            is AnimationAction.UpdateStackPointer ->
                state.copy(
                    stackPointerValue = action.value,
                    stackPointerHighlighted = action.isHighlighted
                )

            is AnimationAction.UpdateStackAnimation ->
                state.copy(
                    stackAnimation = StackAnimationState(
                        operation = action.operation,
                        progress = action.progress,
                        frameLabel = action.frameLabel,
                        frameValue = action.frameValue,
                        registerName = action.registerName
                    )
                )

            is AnimationAction.UpdateAstGrowth ->
                state.copy(currentAst = action.ast, astGrowthProgress = action.progress)

            is AnimationAction.ClearAll ->
                VisualizationUiState(
                    architecture = state.architecture,
                    playbackSpeed = state.playbackSpeed,
                    activeLevels = state.activeLevels,
                    isPanelExpanded = state.isPanelExpanded
                )

            is AnimationAction.UpdateLogicGates ->
                state.copy(
                    logicGateState = LogicGateState(
                        action.gateType,
                        action.leftBits,
                        action.rightBits,
                        action.resultBits,
                        action.signalProgress
                    )
                )

            is AnimationAction.UpdateAluOperation ->
                state.copy(
                    aluOperation = AluOperationVisual(
                        action.operation,
                        action.leftOperand,
                        action.rightOperand,
                        action.result,
                        action.isActive
                    )
                )

            is AnimationAction.UpdateDataPath ->
                state.copy(dataPath = DataPathVisual(action.fromRegister, action.toRegister, action.progress))

            is AnimationAction.UpdateOperatorStack ->
                state.copy(operatorStack = action.stack)

            is AnimationAction.UpdateLinkedList ->
                state.copy(
                    linkedListNodes = action.nodes.map {
                        LinkedListNodeVisual(
                            it.id, it.value, it.nextId, it.isNew, it.isHighlighted, it.isPointer
                        )
                    }
                )

            is AnimationAction.AnimateLinkedListWire ->
                state.copy(
                    linkedListWire = LinkedListWireState(
                        fromNodeId = action.fromNodeId,
                        toNodeId = action.toNodeId,
                        progress = action.progress
                    )
                )

            is AnimationAction.UpdateAddressBus ->
                state.copy(
                    addressBus = AddressBusVisual(
                        action.segment,
                        action.offset,
                        action.fullAddress,
                        action.progress
                    )
                )

            is AnimationAction.UpdateCursorAddress -> {
                val prev = state.cursorMemoryAddress
                state.copy(
                    previousCursorAddress = prev,
                    cursorMemoryAddress = action.address
                )
            }

            is AnimationAction.AnimateMemoryPointer ->
                state.copy(
                    memoryPointerAnimation = MemoryPointerAnimationState(
                        sourceAddress = action.sourceAddress,
                        targetAddress = action.targetAddress,
                        progress = action.progress
                    )
                )

            is AnimationAction.UpdateInstructionPipeline ->
                state.copy(
                    instructionPipeline = action.stages.map {
                        PipelineStageVisual(it.stageName, it.instruction, it.isActive, it.progress)
                    }
                )

            is AnimationAction.UpdateDisplayBuffer ->
                state.copy(
                    displayBuffer = DisplayBufferVisual(action.text, action.cursorPosition, action.isTyping)
                )

            is AnimationAction.UpdateResultFlow ->
                state.copy(resultFlowProgress = action.progress)

            is AnimationAction.UpdateDescription ->
                state.copy(lastEventDescription = action.description)

            else -> state
        }
    }
}
