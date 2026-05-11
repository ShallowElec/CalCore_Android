package com.cloveriris.calcore.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.presentation.visualization.VisualizationUiState
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen
import com.cloveriris.calcore.ui.visualization.VisualizationStage

/**
 * 手机竖屏用的可折叠可视化面板
 *
 * 默认折叠（只显示一条指示条），点击展开显示完整的 VisualizationStage。
 */
@Composable
fun CollapsibleVisualizationPanel(
    state: VisualizationUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlayPause: () -> Unit = {},
    onScrub: (Float) -> Unit = {},
    onRestart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBackground)
    ) {
        // 折叠指示条（始终可见）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "收起可视化" else "展开可视化",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TerminalGray.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = TerminalGray.copy(alpha = 0.7f)
            )
        }

        // 可展开内容
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            VisualizationStage(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                onPlayPause = onPlayPause,
                onScrub = onScrub,
                onRestart = onRestart
            )
        }
    }
}
