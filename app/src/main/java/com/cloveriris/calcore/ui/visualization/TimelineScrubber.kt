package com.cloveriris.calcore.ui.visualization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.LocalVisualizationColors

/**
 * 时间轴控制条
 *
 * @param currentTimeMs 当前时间（毫秒）
 * @param totalTimeMs 总时长（毫秒）
 * @param isPlaying 是否正在播放
 * @param onPlayPause 播放/暂停回调
 * @param onScrub 拖动回调
 * @param onRestart 重新开始回调
 */
@Composable
fun TimelineScrubber(
    currentTimeMs: Long,
    totalTimeMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onScrub: (Float) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viz = LocalVisualizationColors.current
    val progress = if (totalTimeMs > 0) currentTimeMs.toFloat() / totalTimeMs else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 时间显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentTimeMs),
                color = viz.dataPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = formatTime(totalTimeMs),
                color = viz.textMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // 进度条
        Slider(
            value = progress,
            onValueChange = onScrub,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = viz.dataPrimary,
                activeTrackColor = viz.dataPrimary,
                inactiveTrackColor = viz.surface
            )
        )

        // 控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRestart,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = viz.textMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart",
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onPlayPause,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = viz.dataPrimary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    val millis = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, secs, millis)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun TimelineScrubberPreview() {
    CalcoreTheme {
        TimelineScrubber(
            currentTimeMs = 1500,
            totalTimeMs = 5000,
            isPlaying = true,
            onPlayPause = {},
            onScrub = {},
            onRestart = {}
        )
    }
}
