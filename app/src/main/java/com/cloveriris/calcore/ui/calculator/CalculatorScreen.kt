package com.cloveriris.calcore.ui.calculator

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.cloveriris.calcore.domain.model.CalculatorInput
import com.cloveriris.calcore.domain.model.CalculatorMode
import com.cloveriris.calcore.domain.model.SidePanelTab
import com.cloveriris.calcore.presentation.calculator.CalculatorUiState
import com.cloveriris.calcore.presentation.calculator.CalculatorViewModel
import com.cloveriris.calcore.ui.components.DrawerMenu
import com.cloveriris.calcore.ui.components.CalcoreDisplay
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.visualization.VisualizationStage
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onNavigateToGraphing: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu(
                currentMode = uiState.mode,
                onModeSelected = { mode ->
                    viewModel.onModeChange(mode)
                    scope.launch { drawerState.close() }
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {}, // 使用自定义顶部栏
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (isExpanded) {
                LandscapeLayout(
                    uiState = uiState,
                    onInput = viewModel::onInput,
                    onTabChange = viewModel::onTabChange,
                    onRecallHistory = viewModel::recallHistory,
                    onClearHistory = viewModel::clearHistory,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                PortraitLayout(
                    uiState = uiState,
                    onInput = viewModel::onInput,
                    onTabChange = viewModel::onTabChange,
                    onRecallHistory = viewModel::recallHistory,
                    onClearHistory = viewModel::clearHistory,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

// ==================== 竖屏布局 ====================

@Composable
private fun PortraitLayout(
    uiState: CalculatorUiState,
    onInput: (CalculatorInput) -> Unit,
    onTabChange: (SidePanelTab) -> Unit,
    onRecallHistory: (com.cloveriris.calcore.domain.model.HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 自定义顶部栏
        CalculatorHeader(
            mode = uiState.mode,
            activeTab = uiState.activeTab,
            onTabChange = onTabChange,
            onOpenDrawer = onOpenDrawer
        )

        // 显示区
        CalcoreDisplay(
            expression = uiState.state.displayExpression,
            result = uiState.state.displayResult,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Tab 内容区 或 按键区
        when (uiState.activeTab) {
            SidePanelTab.HISTORY -> {
                HistoryPanel(
                    history = uiState.history,
                    onRecall = onRecallHistory,
                    onClear = onClearHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.5f)
                )
            }
            SidePanelTab.MEMORY -> {
                MemoryPanel(
                    memory = uiState.memory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.5f)
                )
            }
            else -> {
                // 按键区
                val keypadModifier = Modifier
                    .fillMaxWidth()
                    .weight(2.5f)
                when (uiState.mode) {
                    CalculatorMode.SCIENTIFIC -> ScientificKeypad(
                        onInput = onInput,
                        hasMemory = uiState.memory.hasValue,
                        modifier = keypadModifier
                    )
                    else -> StandardKeypad(
                        onInput = onInput,
                        hasMemory = uiState.memory.hasValue,
                        modifier = keypadModifier
                    )
                }
            }
        }
    }
}

// ==================== 横屏布局 ====================

@Composable
private fun LandscapeLayout(
    uiState: CalculatorUiState,
    onInput: (CalculatorInput) -> Unit,
    onTabChange: (SidePanelTab) -> Unit,
    onRecallHistory: (com.cloveriris.calcore.domain.model.HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRightExpanded by remember { mutableStateOf(false) }
    val leftWeight by animateFloatAsState(
        targetValue = if (isRightExpanded) 0.40f else 0.55f,
        animationSpec = tween(durationMillis = 300),
        label = "leftWeight"
    )
    val rightWeight by animateFloatAsState(
        targetValue = if (isRightExpanded) 0.60f else 0.45f,
        animationSpec = tween(durationMillis = 300),
        label = "rightWeight"
    )

    Row(modifier = modifier.fillMaxSize()) {
        // 左侧：计算器主体
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(leftWeight)
        ) {
            CalculatorHeader(
                mode = uiState.mode,
                activeTab = SidePanelTab.NONE, // 横屏时顶部不显示 tab
                onTabChange = {},
                onOpenDrawer = onOpenDrawer,
                showTabs = false
            )

            CalcoreDisplay(
                expression = uiState.state.displayExpression,
                result = uiState.state.displayResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            )

            val keypadModifier = Modifier
                .fillMaxWidth()
                .weight(1.4f)
            when (uiState.mode) {
                CalculatorMode.SCIENTIFIC -> ScientificKeypad(
                    onInput = onInput,
                    hasMemory = uiState.memory.hasValue,
                    modifier = keypadModifier
                )
                else -> StandardKeypad(
                    onInput = onInput,
                    hasMemory = uiState.memory.hasValue,
                    modifier = keypadModifier
                )
            }
        }

        // 分隔线
        HorizontalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // 右侧：可展开面板
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(rightWeight)
                .background(TerminalBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isRightExpanded = !isRightExpanded }
                )
                .animateContentSize()
        ) {
            LandscapeSidePanel(
                uiState = uiState,
                isExpanded = isRightExpanded,
                onToggleExpand = { isRightExpanded = !isRightExpanded },
                onTabChange = onTabChange,
                onRecallHistory = onRecallHistory,
                onClearHistory = onClearHistory
            )
        }
    }
}

// ==================== 横屏右侧面板 ====================

@Composable
private fun LandscapeSidePanel(
    uiState: CalculatorUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTabChange: (SidePanelTab) -> Unit,
    onRecallHistory: (com.cloveriris.calcore.domain.model.HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 横屏右侧面板默认展示可视化，但带有切换到历史/内存的 tab
    var rightTab by remember { mutableStateOf(SidePanelTab.VISUALIZATION) }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部 tab 栏 + 展开按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                LandscapeTabButton(
                    label = "可视化",
                    selected = rightTab == SidePanelTab.VISUALIZATION,
                    onClick = { rightTab = SidePanelTab.VISUALIZATION }
                )
                Spacer(modifier = Modifier.width(16.dp))
                LandscapeTabButton(
                    label = "历史记录",
                    selected = rightTab == SidePanelTab.HISTORY,
                    onClick = { rightTab = SidePanelTab.HISTORY }
                )
                Spacer(modifier = Modifier.width(16.dp))
                LandscapeTabButton(
                    label = "内存",
                    selected = rightTab == SidePanelTab.MEMORY,
                    onClick = { rightTab = SidePanelTab.MEMORY }
                )
            }

            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded)
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft
                    else
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "收缩" else "展开",
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                )
            }
        }

        HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))

        // 内容区
        when (rightTab) {
            SidePanelTab.VISUALIZATION -> {
                VisualizationStage(
                    modifier = Modifier.fillMaxSize()
                )
            }
            SidePanelTab.HISTORY -> {
                HistoryPanel(
                    history = uiState.history,
                    onRecall = onRecallHistory,
                    onClear = onClearHistory,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SidePanelTab.MEMORY -> {
                MemoryPanel(
                    memory = uiState.memory,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun LandscapeTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) androidx.compose.ui.graphics.Color.White
            else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
        )
    }
}

// ==================== 通用顶部栏 ====================

@Composable
private fun CalculatorHeader(
    mode: CalculatorMode,
    activeTab: SidePanelTab,
    onTabChange: (SidePanelTab) -> Unit,
    onOpenDrawer: () -> Unit,
    showTabs: Boolean = true
) {
    val modeTitle = when (mode) {
        CalculatorMode.STANDARD -> "标准"
        CalculatorMode.SCIENTIFIC -> "科学"
        CalculatorMode.PROGRAMMER -> "程序员"
        CalculatorMode.DATE -> "日期计算"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：汉堡 + 模式名
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = modeTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 右侧：Tab
        if (showTabs) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeaderTabButton(
                    label = "历史记录",
                    selected = activeTab == SidePanelTab.HISTORY,
                    onClick = {
                        onTabChange(
                            if (activeTab == SidePanelTab.HISTORY) SidePanelTab.NONE
                            else SidePanelTab.HISTORY
                        )
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                HeaderTabButton(
                    label = "内存",
                    selected = activeTab == SidePanelTab.MEMORY,
                    onClick = {
                        onTabChange(
                            if (activeTab == SidePanelTab.MEMORY) SidePanelTab.NONE
                            else SidePanelTab.MEMORY
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Preview ====================

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CalculatorScreenPhonePreview() {
    CalcoreTheme(appTheme = com.cloveriris.calcore.ui.theme.AppTheme.CALCORE_DARK) {
        CalculatorScreen()
    }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 480)
@Composable
private fun CalculatorScreenTabletPreview() {
    CalcoreTheme(appTheme = com.cloveriris.calcore.ui.theme.AppTheme.CALCORE_DARK) {
        CalculatorScreen(windowWidthSizeClass = WindowWidthSizeClass.Expanded)
    }
}
