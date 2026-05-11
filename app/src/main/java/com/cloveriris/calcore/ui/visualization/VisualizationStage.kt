package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 可视化舞台容器
 *
 * 展示底层原理可视化：位格、寄存器、内存网格、栈、时间轴。
 * 背景固定为 TerminalBackground (#0A0A0A)，不受主题切换影响。
 */
@Composable
fun VisualizationStage(
    modifier: Modifier = Modifier,
    demoMode: Boolean = false
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(TerminalBackground)
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        // 标题栏
        Text(
            text = "VISUALIZATION",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (demoMode) {
            VisualizationDemoContent()
        } else {
            VisualizationPlaceholder()
        }
    }
}

@Composable
private fun VisualizationPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
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

@Composable
private fun VisualizationDemoContent() {
    // 64-bit 位格（演示模式）
    val bits = remember {
        List(64) { i -> i % 3 == 0 || i > 55 }
    }

    Text(
        text = "64-BIT REGISTER",
        color = TerminalGray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    BitGrid(bits = bits, layout = BitGridLayout.BAR_1X64)

    Spacer(modifier = Modifier.height(16.dp))

    // 寄存器组
    Text(
        text = "REGISTERS",
        color = TerminalGray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    val registers = remember {
        listOf(
            RegisterVisual("RAX", 0x0000000000000042L, isHighlighted = true),
            RegisterVisual("RBX", 0x0000000000000000L),
            RegisterVisual("RCX", 0x00007FFE_EFBCA000L),
            RegisterVisual("RDX", 0x0000000000000001L),
            RegisterVisual("RSI", 0x0000000000000000L),
            RegisterVisual("RDI", 0x0000000000000000L),
            RegisterVisual("RSP", 0x00007FFE_EFBC9000L),
            RegisterVisual("RBP", 0x00007FFE_EFBC9010L)
        )
    }
    RegisterBank(registers = registers)

    Spacer(modifier = Modifier.height(16.dp))

    // 内存网格
    Text(
        text = "MEMORY LAYOUT",
        color = TerminalGray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    val cells = remember {
        List(24) { i ->
            MemoryCellVisual(
                address = "0x%04X".format(0x1000 + i * 8),
                value = (i * 17 % 256).toByte(),
                isAllocated = i % 5 != 0,
                isPointer = i % 7 == 0 && i != 0
            )
        }
    }
    MemoryGrid(cells = cells, columns = 8)

    Spacer(modifier = Modifier.height(16.dp))

    // 栈视图
    Text(
        text = "STACK",
        color = TerminalGray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    val frames = remember {
        listOf(
            StackFrameVisual("main()", "0x7FFE_EFBC9000"),
            StackFrameVisual("fact(3)", "0x7FFE_EFBC8FE0", isActive = true),
            StackFrameVisual("fact(2)", "0x7FFE_EFBC8FC0", isActive = true),
            StackFrameVisual("fact(1)", "0x7FFE_EFBC8FA0", isActive = true)
        )
    }
    StackView(frames = frames)

    Spacer(modifier = Modifier.height(16.dp))

    // 时间轴
    TimelineScrubber(
        currentTimeMs = 1500,
        totalTimeMs = 5000,
        isPlaying = true,
        onPlayPause = {},
        onScrub = {},
        onRestart = {}
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun VisualizationStagePreview() {
    CalcoreTheme {
        VisualizationStage(
            modifier = Modifier.fillMaxSize(),
            demoMode = true
        )
    }
}
