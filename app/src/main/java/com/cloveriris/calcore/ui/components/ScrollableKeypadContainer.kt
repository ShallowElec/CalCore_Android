package com.cloveriris.calcore.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 带显式滚动条的可滚动按键容器
 *
 * 所有 Keypad 统一使用此容器，确保在横屏/小屏下按键可滚动且滚动条可见。
 */
@Composable
fun ScrollableKeypadContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier) {
        val containerHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }

        // 右侧滚动条
        val showScrollbar by remember(scrollState.maxValue) {
            derivedStateOf { scrollState.maxValue > 0 }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showScrollbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            val contentHeight = scrollState.maxValue + scrollState.viewportSize
            val thumbRatio = scrollState.viewportSize.toFloat() /
                    contentHeight.toFloat().coerceAtLeast(1f)
            val thumbHeight = containerHeight * thumbRatio
            val maxOffset = containerHeight - thumbHeight
            val scrollFraction = if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else 0f

            Box(
                modifier = Modifier
                    .padding(top = 8.dp, end = 3.dp)
                    .width(4.dp)
                    .height(thumbHeight)
                    .offset(y = maxOffset * scrollFraction)
                    .background(
                        Color.White.copy(alpha = 0.35f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
