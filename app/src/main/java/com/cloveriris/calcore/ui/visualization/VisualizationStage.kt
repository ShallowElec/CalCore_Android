package com.cloveriris.calcore.ui.visualization

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.VisualizationLevel
import com.cloveriris.calcore.presentation.visualization.VisualizationUiState
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 可视化舞台容器
 *
 * 展示底层原理可视化 L1-L8：位格、寄存器、内存网格、栈、AST、链表、
 * 运算符栈、指令流水线、显示缓冲区等。
 * 背景固定为 TerminalBackground (#0A0A0A)，不受主题切换影响。
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
            VisualizationLiveContent(state)
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

        // 底部控制条：架构 + 层级切换 + 速度
        BottomControlBar(
            architecture = state.architecture,
            playbackSpeed = state.playbackSpeed,
            activeLevels = state.activeLevels,
            onToggleLevel = onToggleLevel
        )
    }
}

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
            // 播放速度标签
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
private fun VisualizationLiveContent(state: VisualizationUiState) {
    // L1: 布尔代数
    if (state.activeLevels.contains(VisualizationLevel.L1_BOOLEAN_ALGEBRA)
        && state.logicGateState.signalProgress > 0f
    ) {
        SectionTitle("L1: BOOLEAN ALGEBRA")
        Spacer(modifier = Modifier.height(4.dp))
        LogicGateGrid(state = state.logicGateState)
        Spacer(modifier = Modifier.height(8.dp))
    }

    // L2: 数值表示
    if (state.activeLevels.contains(VisualizationLevel.L2_NUMERIC_REPRESENTATION)) {
        SectionTitle("L2: NUMERIC REPRESENTATION")
        Spacer(modifier = Modifier.height(4.dp))
        BitGrid(
            bits = state.bitGridBits,
            layout = BitGridLayout.BAR_1X64,
            highlights = state.bitGridHighlights,
            label = state.bitGridLabel
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // L3: 寄存器与 ALU
    if (state.activeLevels.contains(VisualizationLevel.L3_REGISTERS_ALU)) {
        SectionTitle("L3: REGISTERS & ALU")
        Spacer(modifier = Modifier.height(4.dp))
        if (state.registers.isNotEmpty()) {
            RegisterBank(registers = state.registers, dataPath = state.dataPath)
        } else {
            val defaultRegs = state.architecture.registerNames.map { RegisterVisual(it, 0L) }
            RegisterBank(registers = defaultRegs, dataPath = state.dataPath)
        }
        if (state.aluOperation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AluVisualizer(operation = state.aluOperation)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    // L4: 内存布局 + 栈
    if (state.activeLevels.contains(VisualizationLevel.L4_MEMORY_LAYOUT)) {
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

        // 栈可视化
        SectionTitle("L4: STACK")
        Spacer(modifier = Modifier.height(4.dp))
        StackView(
            frames = state.stackFrames,
            spValue = state.stackPointerValue,
            spHighlighted = state.stackPointerHighlighted,
            stackAnimation = state.stackAnimation,
            spRegisterName = state.architecture.spRegisterName
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // L5: 数据结构
    if (state.activeLevels.contains(VisualizationLevel.L5_DATA_STRUCTURES)) {
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
        Spacer(modifier = Modifier.height(8.dp))
    }

    // L6: 指针与寻址
    if (state.activeLevels.contains(VisualizationLevel.L6_POINTERS_ADDRESSING)
        && state.addressBus != null
    ) {
        SectionTitle("L6: POINTERS & ADDRESSING")
        Spacer(modifier = Modifier.height(4.dp))
        AddressBusView(addressBus = state.addressBus)
        Spacer(modifier = Modifier.height(8.dp))
    }

    // L7: 指令集
    if (state.activeLevels.contains(VisualizationLevel.L7_INSTRUCTION_SET)
        && state.instructionPipeline.isNotEmpty()
    ) {
        SectionTitle("L7: INSTRUCTION PIPELINE")
        Spacer(modifier = Modifier.height(4.dp))
        InstructionPipeline(stages = state.instructionPipeline)
        Spacer(modifier = Modifier.height(8.dp))
    }

    // L8: 结果回显
    if (state.activeLevels.contains(VisualizationLevel.L8_RESULT_DISPLAY)) {
        SectionTitle("L8: RESULT DISPLAY")
        Spacer(modifier = Modifier.height(4.dp))
        DisplayBufferView(buffer = state.displayBuffer)
        Spacer(modifier = Modifier.height(8.dp))
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
                text = "L1: Boolean Algebra  →  L8: Result Display",
                color = TerminalGray.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

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
                totalDurationMs = 3000L,
                currentTimeMs = 1500L,
                isPlaying = true
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}
