package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.VisualizationLevel
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * 可视化舞台底部控制条
 *
 * 固定在所有布局的底部，包含：
 * - 当前模拟架构标签
 * - 激活的可视化层级标签
 */
@Composable
fun BottomControlBar(
    architecture: Architecture,
    activeLevels: Set<VisualizationLevel>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(TerminalBackground)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = architecture.displayName,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TerminalGreen.copy(alpha = 0.8f)
        )

        // L1-L8 激活指示（简略显示）
        val levelStr = VisualizationLevel.entries.joinToString("") {
            if (it in activeLevels) it.shortName.last().toString() else "·"
        }
        Text(
            text = levelStr,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TerminalGray.copy(alpha = 0.6f)
        )
    }
}
