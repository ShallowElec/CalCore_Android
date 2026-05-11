package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground

/**
 * 可视化舞台容器
 *
 * 背景固定为 TerminalBackground (#0A0A0A)，不受主题切换影响。
 * 后续将在此容器内渲染位格、寄存器、内存网格、栈等可视化组件。
 */
@Composable
fun VisualizationStage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(TerminalBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Visualization Stage",
            color = Color(0xFF00FF41),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VisualizationStagePreview() {
    CalcoreTheme {
        VisualizationStage(
            modifier = Modifier.fillMaxSize()
        )
    }
}
