package com.cloveriris.calcore.ui.visualization

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.cloveriris.calcore.ui.theme.TerminalAmber

/**
 * L7 指令流水线可视化（专业时序图风格）
 *
 * 特性：
 * - 4 个阶段横向排列，像 CPU pipeline diagram
 * - 活跃阶段绿色脉冲 + 流动箭头动画
 * - 阶段之间流光连线（活跃时更亮）
 * - 多条指令垂直堆叠，展示流水线深度
 */
@Composable
fun InstructionPipeline(
    stages: List<PipelineStageVisual>,
    modifier: Modifier = Modifier
) {
    val stageOrder = listOf("FETCH", "DECODE", "EXECUTE", "WRITEBACK")
    val grouped = stages.groupBy { it.stageName }
    val infiniteTransition = rememberInfiniteTransition(label = "pipe-flow")
    val flowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flow"
    )

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
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            stageOrder.forEachIndexed { index, stageName ->
                val stageList = grouped[stageName] ?: emptyList()
                val hasActive = stageList.any { it.isActive }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 阶段标题条（活跃时绿色高亮）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (hasActive) TerminalGreen.copy(alpha = 0.15f) else Color(0xFF1A1A1A),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stageName,
                            color = if (hasActive) TerminalGreen else TerminalGray.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (hasActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // 流水线槽位（像工业控制面板的指示灯）
                    stageList.forEach { stage ->
                        PipelineSlot(
                            instruction = stage.instruction,
                            isActive = stage.isActive,
                            progress = stage.progress
                        )
                    }
                    if (stageList.isEmpty()) {
                        // 空槽位占位
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(Color(0xFF111111), RoundedCornerShape(3.dp))
                        )
                    }
                }

                // 阶段间流动箭头
                if (index < stageOrder.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .padding(top = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val nextHasActive = (grouped[stageOrder[index + 1]] ?: emptyList()).any { it.isActive }
                        val arrowAlpha = if (hasActive || nextHasActive) flowAlpha else 0.2f
                        Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                            // 基线
                            drawLine(
                                color = TerminalGreen.copy(alpha = arrowAlpha * 0.4f),
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width - 4.dp.toPx(), size.height / 2),
                                strokeWidth = 1.5f.dp.toPx()
                            )
                            // 箭头头部
                            val tipX = size.width - 4.dp.toPx()
                            val cy = size.height / 2
                            drawLine(
                                color = TerminalGreen.copy(alpha = arrowAlpha),
                                start = Offset(tipX - 4.dp.toPx(), cy - 3.dp.toPx()),
                                end = Offset(tipX, cy),
                                strokeWidth = 1.5f.dp.toPx()
                            )
                            drawLine(
                                color = TerminalGreen.copy(alpha = arrowAlpha),
                                start = Offset(tipX - 4.dp.toPx(), cy + 3.dp.toPx()),
                                end = Offset(tipX, cy),
                                strokeWidth = 1.5f.dp.toPx()
                            )
                            // 活跃时的流光点
                            if (hasActive) {
                                val particleX = (size.width * flowAlpha).coerceAtMost(tipX)
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.8f),
                                    radius = 2.dp.toPx(),
                                    center = Offset(particleX, cy)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部进度条（整体流水线推进感）
        if (stages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            val activeProgress = stages.filter { it.isActive }.map { it.progress }.average().toFloat()
            Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                drawRect(
                    color = Color(0xFF1A1A1A),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height)
                )
                if (activeProgress > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TerminalGreen.copy(alpha = 0.3f),
                                TerminalGreen.copy(alpha = 0.9f),
                                TerminalGreen.copy(alpha = 0.3f)
                            )
                        ),
                        size = androidx.compose.ui.geometry.Size(size.width * activeProgress, size.height)
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineSlot(
    instruction: String,
    isActive: Boolean,
    progress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "slot-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    isActive -> TerminalGreen.copy(alpha = pulseAlpha * 0.85f)
                    instruction.isNotEmpty() -> Color(0xFF1F1F1F)
                    else -> Color(0xFF111111)
                },
                RoundedCornerShape(3.dp)
            )
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = instruction.ifEmpty { "·" },
            color = when {
                isActive -> Color.Black
                instruction.isNotEmpty() -> TerminalGray.copy(alpha = 0.7f)
                else -> TerminalGray.copy(alpha = 0.25f)
            },
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun InstructionPipelinePreview() {
    CalcoreTheme {
        Column {
            InstructionPipeline(
                stages = listOf(
                    PipelineStageVisual("FETCH", "EVAL", isActive = true, progress = 1.0f),
                    PipelineStageVisual("DECODE", "add", isActive = true, progress = 1.0f),
                    PipelineStageVisual("EXECUTE", "add", isActive = true, progress = 0.5f),
                    PipelineStageVisual("WRITEBACK", "", isActive = false, progress = 0f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            InstructionPipeline(stages = emptyList())
        }
    }
}
