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
import androidx.compose.runtime.LaunchedEffect
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
import com.cloveriris.calcore.domain.model.NumberBase
import com.cloveriris.calcore.domain.model.SidePanelTab
import com.cloveriris.calcore.presentation.calculator.CalculatorUiState
import com.cloveriris.calcore.presentation.calculator.CalculatorViewModel
import com.cloveriris.calcore.presentation.visualization.VisualizationUiState
import com.cloveriris.calcore.presentation.visualization.VisualizationViewModel
import com.cloveriris.calcore.ui.components.CalcoreDisplay
import com.cloveriris.calcore.ui.components.DrawerMenu
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.visualization.AstTreeView
import com.cloveriris.calcore.ui.visualization.BottomControlBar
import com.cloveriris.calcore.ui.visualization.VisualizationStage
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onNavigateToGraphing: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = hiltViewModel(),
    visualizationViewModel: VisualizationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val visState by visualizationViewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 桥接 CalculatorViewModel 的 animationEvents → VisualizationViewModel
    LaunchedEffect(Unit) {
        viewModel.animationEvents.collect { event ->
            visualizationViewModel.onEvent(event)
        }
    }

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
            topBar = {},
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (isExpanded) {
                LandscapeLayout(
                    uiState = uiState,
                    visState = visState,
                    onInput = viewModel::onInput,
                    onTabChange = viewModel::onTabChange,
                    onRecallHistory = viewModel::recallHistory,
                    onClearHistory = viewModel::clearHistory,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onToggleVisPanel = { visualizationViewModel.setPanelExpanded(!visState.isPanelExpanded) },
                    onBaseChange = viewModel::onBaseChange,
                    onBitWidthChange = viewModel::onBitWidthChange,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                PortraitLayout(
                    uiState = uiState,
                    visState = visState,
                    onInput = viewModel::onInput,
                    onTabChange = viewModel::onTabChange,
                    onRecallHistory = viewModel::recallHistory,
                    onClearHistory = viewModel::clearHistory,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onToggleVisPanel = { visualizationViewModel.setPanelExpanded(!visState.isPanelExpanded) },
                    onBaseChange = viewModel::onBaseChange,
                    onBitWidthChange = viewModel::onBitWidthChange,
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
    visState: VisualizationUiState,
    onInput: (CalculatorInput) -> Unit,
    onTabChange: (SidePanelTab) -> Unit,
    onRecallHistory: (com.cloveriris.calcore.domain.model.HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleVisPanel: () -> Unit,
    onBaseChange: (NumberBase) -> Unit,
    onBitWidthChange: (com.cloveriris.calcore.domain.model.BitWidth) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 顶部栏
        CalculatorHeader(
            mode = uiState.mode,
            activeTab = uiState.activeTab,
            tabs = listOf(SidePanelTab.HISTORY, SidePanelTab.MEMORY),
            onTabChange = onTabChange,
            onOpenDrawer = onOpenDrawer
        )

        // 可折叠可视化面板（仅当 Tab 为 NONE 或 VISUALIZATION 时显示）
        if (uiState.activeTab == SidePanelTab.NONE || uiState.activeTab == SidePanelTab.VISUALIZATION) {
            CollapsibleVisualizationPanel(
                state = visState,
                isExpanded = visState.isPanelExpanded,
                onToggleExpand = onToggleVisPanel
            )
        }

        // 显示区
        if (uiState.mode == CalculatorMode.PROGRAMMER) {
            ProgrammerDisplay(
                value = uiState.programmerValue,
                base = uiState.numberBase,
                bitWidth = uiState.bitWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
            )
        } else {
            CalcoreDisplay(
                expression = uiState.state.displayExpression,
                result = uiState.state.displayResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
            )
        }

        // 内容区：按键 / 历史 / 内存 / 可视化
        when (uiState.activeTab) {
            SidePanelTab.HISTORY -> {
                HistoryPanel(
                    history = uiState.history,
                    onRecall = onRecallHistory,
                    onClear = onClearHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.2f)
                )
            }
            SidePanelTab.MEMORY -> {
                MemoryPanel(
                    memory = uiState.memory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2.2f)
                )
            }
            SidePanelTab.VISUALIZATION -> {
                // 可视化 Tab 已移除，回退到按键
                onTabChange(SidePanelTab.NONE)
                val keypadModifier = Modifier
                    .fillMaxWidth()
                    .weight(2.2f)
                when (uiState.mode) {
                    CalculatorMode.SCIENTIFIC -> ScientificKeypad(
                        onInput = onInput,
                        hasMemory = uiState.memory.hasValue,
                        modifier = keypadModifier
                    )
                    CalculatorMode.PROGRAMMER -> ProgrammerKeypad(
                        currentBase = uiState.numberBase,
                        currentBitWidth = uiState.bitWidth,
                        onInput = onInput,
                        onBaseChange = onBaseChange,
                        onBitWidthChange = onBitWidthChange,
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
            else -> {
                // 按键区
                val keypadModifier = Modifier
                    .fillMaxWidth()
                    .weight(2.2f)
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

        // 底部控制条
        BottomControlBar(
            architecture = visState.architecture,
            activeLevels = visState.activeLevels
        )
    }
}

// ==================== 横屏布局 ====================

@Composable
private fun LandscapeLayout(
    uiState: CalculatorUiState,
    visState: VisualizationUiState,
    onInput: (CalculatorInput) -> Unit,
    onTabChange: (SidePanelTab) -> Unit,
    onRecallHistory: (com.cloveriris.calcore.domain.model.HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleVisPanel: () -> Unit,
    onBaseChange: (NumberBase) -> Unit,
    onBitWidthChange: (com.cloveriris.calcore.domain.model.BitWidth) -> Unit,
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

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            // 左侧：计算器主体
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(leftWeight)
            ) {
                CalculatorHeader(
                    mode = uiState.mode,
                    activeTab = uiState.activeTab,
                    tabs = listOf(SidePanelTab.HISTORY, SidePanelTab.MEMORY),
                    onTabChange = onTabChange,
                    onOpenDrawer = onOpenDrawer
                )

                if (uiState.mode == CalculatorMode.PROGRAMMER) {
                    ProgrammerDisplay(
                        value = uiState.programmerValue,
                        base = uiState.numberBase,
                        bitWidth = uiState.bitWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                    )
                } else {
                    CalcoreDisplay(
                        expression = uiState.state.displayExpression,
                        result = uiState.state.displayResult,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                    )
                }

                // 横屏时左侧也显示历史/内存小面板（可切换）
                when (uiState.activeTab) {
                    SidePanelTab.HISTORY -> {
                        HistoryPanel(
                            history = uiState.history,
                            onRecall = onRecallHistory,
                            onClear = onClearHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.4f)
                        )
                    }
                    SidePanelTab.MEMORY -> {
                        MemoryPanel(
                            memory = uiState.memory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.4f)
                        )
                    }
                    else -> { /* 占位，不占空间 */ }
                }

                val keypadModifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                when (uiState.mode) {
                    CalculatorMode.SCIENTIFIC -> ScientificKeypad(
                        onInput = onInput,
                        hasMemory = uiState.memory.hasValue,
                        modifier = keypadModifier
                    )
                    CalculatorMode.PROGRAMMER -> ProgrammerKeypad(
                        currentBase = uiState.numberBase,
                        currentBitWidth = uiState.bitWidth,
                        onInput = onInput,
                        onBaseChange = onBaseChange,
                        onBitWidthChange = onBitWidthChange,
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

            // 右侧：可视化舞台（点击可展开）
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // 展开/收缩按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { isRightExpanded = !isRightExpanded }) {
                            Icon(
                                imageVector = if (isRightExpanded)
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                else
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isRightExpanded) "收缩" else "展开",
                                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // 可视化内容
                    VisualizationStage(
                        state = visState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // 底部控制条
                    BottomControlBar(
                        architecture = visState.architecture,
                        activeLevels = visState.activeLevels
                    )
                }
            }
        }
    }
}

// ==================== 通用顶部栏 ====================

@Composable
private fun CalculatorHeader(
    mode: CalculatorMode,
    activeTab: SidePanelTab,
    tabs: List<SidePanelTab>,
    onTabChange: (SidePanelTab) -> Unit,
    onOpenDrawer: () -> Unit
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { tab ->
                val label = when (tab) {
                    SidePanelTab.HISTORY -> "历史记录"
                    SidePanelTab.MEMORY -> "内存"
                    else -> ""
                }
                if (label.isEmpty()) return@forEach
                val selected = activeTab == tab
                TextButton(
                    onClick = {
                        onTabChange(if (selected) SidePanelTab.NONE else tab)
                    },
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
                if (tab != tabs.last()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

// ==================== 程序员模式显示区 ====================

@Composable
private fun ProgrammerDisplay(
    value: Long,
    base: NumberBase,
    bitWidth: com.cloveriris.calcore.domain.model.BitWidth,
    modifier: Modifier = Modifier
) {
    val displayValue = when (base) {
        NumberBase.BIN -> "${base.prefix}${java.lang.Long.toBinaryString(value)}"
        NumberBase.OCT -> "${base.prefix}${java.lang.Long.toOctalString(value)}"
        NumberBase.DEC -> value.toString()
        NumberBase.HEX -> "${base.prefix}${java.lang.Long.toHexString(value).uppercase()}"
    }
    val secondaryInfo = "${bitWidth.bits}-bit | ${NumberBase.HEX.prefix}${java.lang.Long.toHexString(value).uppercase().padStart(bitWidth.bits / 4, '0')}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = secondaryInfo,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = displayValue,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1
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
