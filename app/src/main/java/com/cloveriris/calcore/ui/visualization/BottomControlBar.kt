package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.VisualizationLevel
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 可视化舞台底部控制条
 *
 * 固定在所有布局的底部，包含：
 * - 当前模拟架构标签
 * - 播放速度指示
 * - 可点击的 L1-L8 层级切换按钮
 */
@Composable
fun BottomControlBar(
    architecture: Architecture,
    activeLevels: Set<VisualizationLevel>,
    playbackSpeed: Float = 1.0f,
    onToggleLevel: (VisualizationLevel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(TerminalBackground)
            .padding(horizontal = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 架构标签
        Text(
            text = architecture.displayName,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TerminalGreen.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // 速度标签
        Text(
            text = "${playbackSpeed}x",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TerminalGray.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // L1-L8 可点击层级标签
        VisualizationLevel.entries.forEach { level ->
            val isActive = level in activeLevels
            Box(
                modifier = Modifier
                    .clickable { onToggleLevel(level) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.shortName,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isActive) TerminalGreen else TerminalGray.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun BottomControlBarPreview() {
    CalcoreTheme {
        Column {
            BottomControlBar(
                architecture = Architecture.X86_64,
                activeLevels = VisualizationLevel.entries.toSet(),
                playbackSpeed = 1.0f
            )
            Spacer(modifier = Modifier.height(8.dp))
            BottomControlBar(
                architecture = Architecture.ARM64,
                activeLevels = setOf(
                    VisualizationLevel.L1_BOOLEAN_ALGEBRA,
                    VisualizationLevel.L3_REGISTERS_ALU,
                    VisualizationLevel.L8_RESULT_DISPLAY
                ),
                playbackSpeed = 0.5f
            )
        }
    }
}
