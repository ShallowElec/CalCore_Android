package com.cloveriris.calcore.presentation.visualization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloveriris.calcore.data.local.SettingsDataStore
import com.cloveriris.calcore.domain.model.AnimationAction
import com.cloveriris.calcore.domain.model.AnimationEvent
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.PipelinePhase
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
 *
 * 核心特性：阶段式流水线播放。按下 = 后，L1-L8 被映射为 6 个阶段，
 * 视觉焦点逐阶段推进，当前阶段展开，已完成阶段收缩为摘要。
 */
@HiltViewModel
class VisualizationViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisualizationUiState())
    val uiState: StateFlow<VisualizationUiState> = _uiState.asStateFlow()

    private var currentScript: com.cloveriris.calcore.domain.model.AnimationScript? = null
    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            settingsDataStore.architecture.collect { arch ->
                _uiState.update { it.copy(architecture = arch) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.playbackSpeed.collect { speed ->
                _uiState.update { it.copy(playbackSpeed = speed) }
            }
        }
    }

    /** 基准 step 间隔（毫秒），默认 200ms */
    private val baseStepIntervalMs = 200L

    /** 阶段之间的停顿感（毫秒） */
    private val phaseGapMs = 120L

    fun onEvent(event: AnimationEvent) {
        val arch = _uiState.value.architecture
        val script = VisualizationEngine.generateScript(event, arch)
        currentScript = script
        _uiState.update {
            // 保留用户设置，重置阶段状态，避免 placeholder → content 闪烁
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
        viewModelScope.launch {
            settingsDataStore.setArchitecture(arch)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val coerced = speed.coerceIn(0.2f, 2.0f)
        _uiState.update { it.copy(playbackSpeed = coerced) }
        viewModelScope.launch {
            settingsDataStore.setPlaybackSpeed(coerced)
        }
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
            val phaseGap = (phaseGapMs / speed).toLong().coerceAtLeast(30L)

            // 预计算阶段区间
            val phaseRanges = computePhaseRanges(script)

            repeat(stepCount) { i ->
                delay(stepDelay)
                val progress = (i + 1) / stepCount.toFloat()

                // 检测是否跨越阶段边界，如果是则增加额外停顿
                val currentPhase = detectPhaseAtProgress(progress, phaseRanges)
                val prevPhase = detectPhaseAtProgress(((i).coerceAtLeast(0)) / stepCount.toFloat(), phaseRanges)
                if (currentPhase != null && currentPhase != prevPhase && prevPhase != null) {
                    delay(phaseGap)
                }

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
            val phaseGap = (phaseGapMs / speed).toLong().coerceAtLeast(30L)
            val phaseRanges = computePhaseRanges(script)
            val remainingSteps = ((1f - currentProgress) * stepCount).toInt().coerceAtLeast(1)

            repeat(remainingSteps) { i ->
                delay(stepDelay)
                val progress = currentProgress + (i + 1) / stepCount.toFloat() * (1f - currentProgress)

                val currentPhase = detectPhaseAtProgress(progress, phaseRanges)
                val prevPhase = detectPhaseAtProgress(
                    (currentProgress + i / stepCount.toFloat() * (1f - currentProgress)).coerceAtLeast(0f),
                    phaseRanges
                )
                if (currentPhase != null && currentPhase != prevPhase && prevPhase != null) {
                    delay(phaseGap)
                }

                applyScriptAtProgress(progress, script)
            }
            _uiState.update { it.copy(isPlaying = false, evaluationProgress = 1f) }
        }
    }

    /**
     * 从脚本中提取每个阶段的 [startProgress, endProgress) 区间。
     */
    private fun computePhaseRanges(
        script: com.cloveriris.calcore.domain.model.AnimationScript
    ): Map<PipelinePhase, Pair<Float, Float>> {
        val total = script.totalDurationMs.toFloat().coerceAtLeast(1f)
        val ranges = mutableMapOf<PipelinePhase, Pair<Float, Float>>()
        var currentPhase: PipelinePhase? = null
        var phaseStart = 0f

        val sortedEvents = script.events.sortedBy { it.timeMs }

        for (event in sortedEvents) {
            val progress = event.timeMs / total
            when (val action = event.action) {
                is AnimationAction.EnterPhase -> {
                    if (currentPhase != null) {
                        ranges[currentPhase] = phaseStart to progress
                    }
                    currentPhase = action.phase
                    phaseStart = progress
                }
                is AnimationAction.ExitPhase -> {
                    if (currentPhase != null) {
                        ranges[currentPhase] = phaseStart to progress
                        currentPhase = null
                    }
                }
                else -> { /* ignore */ }
            }
        }
        // 最后一个阶段如果未 exit，则延伸到结尾
        if (currentPhase != null) {
            ranges[currentPhase] = phaseStart to 1.0f
        }
        return ranges
    }

    private fun detectPhaseAtProgress(
        progress: Float,
        ranges: Map<PipelinePhase, Pair<Float, Float>>
    ): PipelinePhase? {
        return ranges.entries.find { progress >= it.value.first && progress < it.value.second }?.key
    }

    private fun applyScriptAtProgress(
        progress: Float,
        script: com.cloveriris.calcore.domain.model.AnimationScript
    ) {
        val timeMs = (progress * script.totalDurationMs).toLong()
        val actions = script.events.filter { it.timeMs <= timeMs }.map { it.action }

        // 计算阶段相关状态
        val ranges = computePhaseRanges(script)
        val activePhase = detectPhaseAtProgress(progress, ranges)
        val phaseProgress = activePhase?.let { phase ->
            val range = ranges[phase] ?: return@let 0f
            val duration = range.second - range.first
            if (duration > 0.001f) ((progress - range.first) / duration).coerceIn(0f, 1f) else 1f
        } ?: 0f
        val completedPhases = ranges.filter { progress >= it.value.second }.keys
        val phaseDurations = ranges.mapValues { ((it.value.second - it.value.first) * script.totalDurationMs).toLong() }

        var newState = _uiState.value.copy(
            evaluationProgress = progress,
            currentTimeMs = timeMs,
            totalDurationMs = script.totalDurationMs,
            activePhase = activePhase,
            phaseProgress = phaseProgress,
            completedPhases = completedPhases,
            phaseDurations = phaseDurations
        )

        for (action in actions) {
            newState = reduceAction(newState, action)
        }
        _uiState.value = newState.copy(isPlaying = _uiState.value.isPlaying)
    }

    // ==================== 状态归约器 ====================

    private fun reduceAction(state: VisualizationUiState, action: AnimationAction): VisualizationUiState {
        return when (action) {
            is AnimationAction.EnterPhase ->
                state.copy(
                    activePhase = action.phase,
                    phaseProgress = 0f,
                    completedPhases = state.completedPhases - action.phase
                )

            is AnimationAction.ExitPhase -> {
                val newCompleted = state.completedPhases + action.phase
                val newSnapshots = if (action.summary.isNotBlank()) {
                    state.phaseSnapshots + (action.phase to action.summary)
                } else {
                    state.phaseSnapshots - action.phase
                }
                state.copy(
                    activePhase = if (state.activePhase == action.phase) null else state.activePhase,
                    completedPhases = newCompleted,
                    phaseSnapshots = newSnapshots
                )
            }

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
                } else if (action.index < state.architecture.registerNames.size) {
                    // 初始化缺失的寄存器到目标索引
                    while (regs.size <= action.index) {
                        val name = state.architecture.registerNames.getOrNull(regs.size) ?: "R${regs.size}"
                        regs.add(RegisterVisual(name, 0L))
                    }
                    regs[action.index] = regs[action.index].copy(
                        value = action.value,
                        isHighlighted = action.isHighlighted
                    )
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
