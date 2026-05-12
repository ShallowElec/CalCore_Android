package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.PipelinePhase
import com.cloveriris.calcore.domain.model.VisualizationLevel
import com.cloveriris.calcore.domain.model.toVisualizationLevels
import com.cloveriris.calcore.presentation.visualization.VisualizationUiState
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 可视化舞台容器 —— 阶段式流水线布局
 *
 * 按下 = 后，L1-L8 被映射为 6 个阶段，像机械流水线一样从上到下逐阶段执行：
 * - 当前活跃阶段：完整展开，绿色脉冲边框，正常亮度
 * - 已完成阶段：收缩为单行摘要，亮度 40%，细虚线分隔
 * - 未开始阶段：不显示
 */
@Composable
fun VisualizationStage(
    state: VisualizationUiState,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit = {},
    onScrub: (Float) -> Unit = {},
    onRestart: () -> Unit = {},
    onToggleLevel: (VisualizationLevel) -> Unit = {}
) {
    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(0)
    }

    Column(
        modifier = modifier
            .background(TerminalBackground)
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        // 标题栏 + 事件描述
        RowTitle(state)

        Spacer(modifier = Modifier.height(12.dp))

        // 如果还没有任何事件且不在播放中，显示占位提示
        if (state.lastEventDescription.isEmpty() && state.evaluationProgress == 0f && !state.isPlaying) {
            VisualizationPlaceholder()
        } else {
            PhasePipelineContent(state)
        }

        // 时间轴控制条（底部固定）
        if (state.totalDurationMs > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            TimelineScrubber(
                currentTimeMs = state.currentTimeMs,
                totalTimeMs = state.totalDurationMs,
                isPlaying = state.isPlaying,
                onPlayPause = onPlayPause,
                onScrub = onScrub,
                onRestart = onRestart
            )
        }

        // 底部控制条：阶段指示灯 + 架构 + 层级切换 + 速度
        BottomControlBar(
            architecture = state.architecture,
            playbackSpeed = state.playbackSpeed,
            activeLevels = state.activeLevels,
            activePhase = state.activePhase,
            completedPhases = state.completedPhases,
            onToggleLevel = onToggleLevel
        )
    }
}

@Composable
private fun PhasePipelineContent(state: VisualizationUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val orderedPhases = PipelinePhase.orderedValues()

        orderedPhases.forEachIndexed { index, phase ->
            val isActive = state.activePhase == phase
            val isCompleted = phase in state.completedPhases
            val isPending = !isActive && !isCompleted

            // 未开始阶段：完全不显示（或显示极简占位）
            if (isPending && state.activePhase == null && state.evaluationProgress == 0f) {
                // 初始状态：全部隐藏
                return@forEachIndexed
            }
            if (isPending && state.activePhase != null && phase.order > state.activePhase.order) {
                // 播放中：未到达的阶段隐藏
                return@forEachIndexed
            }

            when {
                isActive -> ActivePhaseCard(
                    phase = phase,
                    orderNum = index + 1,
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )
                isCompleted -> CompletedPhaseCard(
                    phase = phase,
                    orderNum = index + 1,
                    summary = state.phaseSnapshots[phase] ?: "",
                    modifier = Modifier.fillMaxWidth()
                )
                // 未开始但在已完成的后面：显示极淡的占位线
                else -> PendingPhaseLine(
                    phase = phase,
                    orderNum = index + 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 阶段之间用细虚线分隔（已完成→任何状态之间）
            if (isCompleted || (isActive && state.completedPhases.isNotEmpty())) {
                PhaseDivider()
            }
        }
    }
}

// ==================== 活跃阶段卡片（完整展开） ====================

@Composable
private fun ActivePhaseCard(
    phase: PipelinePhase,
    orderNum: Int,
    state: VisualizationUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .animateContentSize(animationSpec = tween(300))
            .background(TerminalGreen.copy(alpha = 0.04f))
            .padding(8.dp)
    ) {
        // 阶段标题行：序号 + 名称 + 进度条
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 序号标签
            Box(
                modifier = Modifier
                    .background(TerminalGreen.copy(alpha = 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "%02d".format(orderNum),
                    color = TerminalGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 阶段名
            Text(
                text = phase.displayName.uppercase(),
                color = TerminalGreen,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )

            // 进度百分比
            Text(
                text = "${(state.phaseProgress * 100).toInt()}%",
                color = TerminalGreen.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // 左侧绿色竖线指示
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeightForPhase(phase, state))
                    .background(TerminalGreen.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                PhaseVisualContent(phase, state, isActive = true)
            }
        }
    }
}

@Composable
private fun animatedHeightForPhase(phase: PipelinePhase, state: VisualizationUiState): androidx.compose.ui.unit.Dp {
    // 根据阶段内容估算高度，活跃时给足够空间
    return when (phase) {
        PipelinePhase.PHASE_INPUT -> 120.dp
        PipelinePhase.PHASE_REGISTERS -> 380.dp
        PipelinePhase.PHASE_MEMORY -> 320.dp
        PipelinePhase.PHASE_PARSE -> 280.dp
        PipelinePhase.PHASE_EXECUTE -> 240.dp
        PipelinePhase.PHASE_OUTPUT -> 100.dp
        else -> 80.dp
    }
}

@Composable
private fun PhaseVisualContent(
    phase: PipelinePhase,
    state: VisualizationUiState,
    isActive: Boolean
) {
    val levels = phase.toVisualizationLevels()

    when (phase) {
        PipelinePhase.PHASE_INPUT -> {
            // L1: 布尔代数（仅当逻辑门有信号时显示）
            if (levels.contains(VisualizationLevel.L1_BOOLEAN_ALGEBRA)
                && state.logicGateState.signalProgress > 0f
            ) {
                SectionTitle("L1: BOOLEAN ALGEBRA")
                Spacer(modifier = Modifier.height(4.dp))
                LogicGateGrid(state = state.logicGateState)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // L2: 数值表示
            if (levels.contains(VisualizationLevel.L2_NUMERIC_REPRESENTATION)) {
                SectionTitle("L2: NUMERIC REPRESENTATION")
                Spacer(modifier = Modifier.height(4.dp))
                BitGrid(
                    bits = state.bitGridBits,
                    layout = BitGridLayout.BAR_1X64,
                    highlights = state.bitGridHighlights,
                    label = state.bitGridLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.bitGridLabel,
                    color = TerminalGray.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        PipelinePhase.PHASE_REGISTERS -> {
            // L3: 寄存器与 ALU
            if (levels.contains(VisualizationLevel.L3_REGISTERS_ALU)) {
                SectionTitle("L3: REGISTERS & ALU")
                Spacer(modifier = Modifier.height(4.dp))
                // 初始化完整寄存器组
                val fullRegs = if (state.registers.isEmpty()) {
                    state.architecture.registerNames.map { RegisterVisual(it, 0L) }
                } else {
                    // 合并默认值与当前值
                    state.architecture.registerNames.mapIndexed { index, name ->
                        state.registers.getOrNull(index) ?: RegisterVisual(name, 0L)
                    }
                }
                RegisterBank(registers = fullRegs, dataPath = state.dataPath)
                Spacer(modifier = Modifier.height(8.dp))
                if (state.aluOperation != null) {
                    AluVisualizer(operation = state.aluOperation)
                } else {
                    // ALU 待机占位
                    AluVisualizer(operation = null)
                }
            }
        }

        PipelinePhase.PHASE_MEMORY -> {
            // L4: 内存布局 + 栈
            if (levels.contains(VisualizationLevel.L4_MEMORY_LAYOUT)) {
                if (state.memoryCells.isNotEmpty()) {
                    SectionTitle("L4: MEMORY LAYOUT")
                    Spacer(modifier = Modifier.height(4.dp))
                    MemoryGrid(
                        cells = state.memoryCells,
                        columns = 8,
                        cursorAddress = state.cursorMemoryAddress,
                        previousCursorAddress = state.previousCursorAddress,
                        pointerAnimation = state.memoryPointerAnimation
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                SectionTitle("L4: STACK")
                Spacer(modifier = Modifier.height(4.dp))
                StackView(
                    frames = state.stackFrames,
                    spValue = state.stackPointerValue,
                    spHighlighted = state.stackPointerHighlighted,
                    stackAnimation = state.stackAnimation,
                    spRegisterName = state.architecture.spRegisterName
                )
            }
        }

        PipelinePhase.PHASE_PARSE -> {
            // L5: 数据结构
            if (levels.contains(VisualizationLevel.L5_DATA_STRUCTURES)) {
                SectionTitle("L5: DATA STRUCTURES")
                Spacer(modifier = Modifier.height(4.dp))
                if (state.currentAst != null) {
                    AstTreeView(
                        expression = state.currentAst,
                        growthProgress = state.astGrowthProgress
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OperatorStackView(stack = state.operatorStack)
                Spacer(modifier = Modifier.height(8.dp))
                LinkedListView(
                    nodes = state.linkedListNodes,
                    wireState = state.linkedListWire
                )
            }
        }

        PipelinePhase.PHASE_EXECUTE -> {
            // L6: 指针与寻址
            if (levels.contains(VisualizationLevel.L6_POINTERS_ADDRESSING)
                && state.addressBus != null
            ) {
                SectionTitle("L6: POINTERS & ADDRESSING")
                Spacer(modifier = Modifier.height(4.dp))
                AddressBusView(addressBus = state.addressBus)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // L7: 指令集
            if (levels.contains(VisualizationLevel.L7_INSTRUCTION_SET)
                && state.instructionPipeline.isNotEmpty()
            ) {
                SectionTitle("L7: INSTRUCTION PIPELINE")
                Spacer(modifier = Modifier.height(4.dp))
                InstructionPipeline(stages = state.instructionPipeline)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // ALU 运算（再次展示，强调执行）
            if (state.aluOperation != null && state.aluOperation.isActive) {
                SectionTitle("L7: ALU EXECUTE")
                Spacer(modifier = Modifier.height(4.dp))
                AluVisualizer(operation = state.aluOperation)
            }
        }

        PipelinePhase.PHASE_OUTPUT -> {
            // L8: 结果回显
            if (levels.contains(VisualizationLevel.L8_RESULT_DISPLAY)) {
                SectionTitle("L8: RESULT DISPLAY")
                Spacer(modifier = Modifier.height(4.dp))
                if (state.displayBuffer != null) {
                    DisplayBufferView(buffer = state.displayBuffer)
                }
                if (state.resultFlowProgress > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ResultFlowIndicator(progress = state.resultFlowProgress)
                }
            }
        }

        else -> { /* IDLE 无内容 */ }
    }
}

@Composable
private fun ResultFlowIndicator(progress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("REGISTER", color = TerminalGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Box(modifier = Modifier.weight(1f).height(2.dp).background(TerminalGray.copy(alpha = 0.2f))) {
            Box(modifier = Modifier.fillMaxWidth(progress).height(2.dp).background(TerminalGreen))
        }
        Text("DISPLAY", color = TerminalGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

// ==================== 已完成阶段卡片（单行摘要） ====================

@Composable
private fun CompletedPhaseCard(
    phase: PipelinePhase,
    orderNum: Int,
    summary: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        Row(
            modifier = modifier
                .animateContentSize(animationSpec = tween(300))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 序号（暗淡绿色）
            Text(
                text = "%02d".format(orderNum),
                color = TerminalGreen.copy(alpha = 0.3f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            // 阶段名
            Text(
                text = phase.displayName.uppercase(),
                color = TerminalGray.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            // 摘要
            if (summary.isNotBlank()) {
                Text(
                    text = "▸ $summary",
                    color = TerminalGray.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }

            // 完成标记
            Text(
                text = "✓",
                color = TerminalGreen.copy(alpha = 0.3f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==================== 未开始阶段占位线 ====================

@Composable
private fun PendingPhaseLine(
    phase: PipelinePhase,
    orderNum: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "%02d".format(orderNum),
            color = TerminalGray.copy(alpha = 0.15f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = phase.displayName.uppercase(),
            color = TerminalGray.copy(alpha = 0.2f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier.weight(1f).height(1.dp)
                .background(TerminalGray.copy(alpha = 0.1f))
        )
    }
}

// ==================== 阶段分隔线 ====================

@Composable
private fun PhaseDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        thickness = 0.5.dp,
        color = TerminalGray.copy(alpha = 0.15f)
    )
}

// ==================== 通用组件 ====================

@Composable
private fun RowTitle(state: VisualizationUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "VISUALIZATION",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${state.playbackSpeed}x",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TerminalGray.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (state.lastEventDescription.isNotEmpty()) {
                Text(
                    text = state.lastEventDescription,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalGreen.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TerminalGray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp
    )
}

@Composable
private fun VisualizationPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Press any key to visualize",
                color = TerminalGreen.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "01 INPUT → 02 REGISTERS → 03 MEMORY → 04 PARSE → 05 EXECUTE → 06 OUTPUT",
                color = TerminalGray.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==================== Preview ====================

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun VisualizationStagePreview() {
    CalcoreTheme {
        VisualizationStage(
            state = VisualizationUiState(
                bitGridBits = List(64) { i -> i % 3 == 0 || i > 55 },
                bitGridLabel = "ASCII '5' = 0x35",
                registers = listOf(
                    RegisterVisual("RAX", 0x35, isHighlighted = true),
                    RegisterVisual("RBX", 0x00),
                    RegisterVisual("RCX", 0x00),
                    RegisterVisual("RDX", 0x00)
                ),
                stackFrames = listOf(
                    StackFrameVisual("main()", "0x7FFE_EFBC9000"),
                    StackFrameVisual("fact(3)", "0x7FFE_EFBC8FE0", isActive = true)
                ),
                stackPointerValue = "0x7FFE_EFBC8FE0",
                stackPointerHighlighted = true,
                memoryCells = List(16) { i ->
                    MemoryCellVisual(
                        address = "0x%04X".format(0x1000 + i * 8),
                        value = (i * 17 % 256).toByte(),
                        isAllocated = i % 3 != 0,
                        isPointer = i == 4
                    )
                },
                cursorMemoryAddress = 0x1008,
                lastEventDescription = "输入数字: 5",
                totalDurationMs = 5000L,
                currentTimeMs = 2500L,
                isPlaying = true,
                activePhase = PipelinePhase.PHASE_MEMORY,
                phaseProgress = 0.6f,
                completedPhases = setOf(PipelinePhase.PHASE_INPUT, PipelinePhase.PHASE_REGISTERS),
                phaseSnapshots = mapOf(
                    PipelinePhase.PHASE_INPUT to "L1-L2: 5.0, 3.0 loaded",
                    PipelinePhase.PHASE_REGISTERS to "L3: RAX←5, RBX←3"
                )
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}
