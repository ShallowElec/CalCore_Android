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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.presentation.visualization.VisualizationUiState
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 可视化舞台容器
 *
 * 展示底层原理可视化：位格、寄存器、内存网格、栈。
 * 背景固定为 TerminalBackground (#0A0A0A)，不受主题切换影响。
 */
@Composable
fun VisualizationStage(
    state: VisualizationUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(TerminalBackground)
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        // 标题栏 + 事件描述
        RowTitle(state)

        Spacer(modifier = Modifier.height(12.dp))

        // 如果还没有任何事件，显示占位提示
        if (state.lastEventDescription.isEmpty()) {
            VisualizationPlaceholder()
        } else {
            VisualizationLiveContent(state)
        }
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

@Composable
private fun VisualizationLiveContent(state: VisualizationUiState) {
    // 64-bit 位格
    Text(
        text = state.bitGridLabel.ifEmpty { "64-BIT REGISTER" },
        color = TerminalGray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    BitGrid(bits = state.bitGridBits, layout = BitGridLayout.BAR_1X64)

    Spacer(modifier = Modifier.height(16.dp))

    // 寄存器组
    Text(
        text = "REGISTERS",
        color = TerminalGray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    if (state.registers.isNotEmpty()) {
        RegisterBank(registers = state.registers)
    } else {
        val defaultRegs = state.architecture.registerNames.map { RegisterVisual(it, 0L) }
        RegisterBank(registers = defaultRegs)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 内存网格（仅当有数据时显示，否则折叠节省空间）
    if (state.memoryCells.isNotEmpty()) {
        Text(
            text = "MEMORY LAYOUT",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        MemoryGrid(cells = state.memoryCells, columns = 8)
        Spacer(modifier = Modifier.height(16.dp))
    }

    // 栈视图（仅当有数据时显示）
    if (state.stackFrames.isNotEmpty()) {
        Text(
            text = "STACK",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        StackView(frames = state.stackFrames)
        Spacer(modifier = Modifier.height(16.dp))
    }
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
                lastEventDescription = "输入数字: 5"
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}
