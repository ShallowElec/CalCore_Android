package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.presentation.visualization.PipelineStageVisual
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen

/**
 * L7 指令流水线可视化
 *
 * 4 个阶段横向排列，支持同一阶段内多条指令垂直堆叠。
 */
@Composable
fun InstructionPipeline(
    stages: List<PipelineStageVisual>,
    modifier: Modifier = Modifier
) {
    val stageOrder = listOf("FETCH", "DECODE", "EXECUTE", "WRITEBACK")
    val grouped = stages.groupBy { it.stageName }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .padding(12.dp)
    ) {
        Text(
            text = "L7: INSTRUCTION PIPELINE",
            color = TerminalGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stageOrder.forEachIndexed { index, stageName ->
                val stageList = grouped[stageName] ?: emptyList()

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    stageList.forEach { stage ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (stage.isActive) TerminalGreen else Color(0xFF1F1F1F),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stage.instruction.ifEmpty { "·" },
                                color = if (stage.isActive) Color.Black else TerminalGray,
                                fontSize = 10.sp,
                                fontWeight = if (stage.isActive) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = stageName,
                        color = TerminalGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (index < stageOrder.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "→",
                            color = TerminalGreen,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun InstructionPipelinePreview() {
    CalcoreTheme {
        Column {
            InstructionPipeline(
                stages = listOf(
                    PipelineStageVisual("FETCH", "add", isActive = true),
                    PipelineStageVisual("DECODE", "add", isActive = true),
                    PipelineStageVisual("EXECUTE", "mul", isActive = true),
                    PipelineStageVisual("WRITEBACK", "push", isActive = false)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            InstructionPipeline(stages = emptyList())
        }
    }
}
