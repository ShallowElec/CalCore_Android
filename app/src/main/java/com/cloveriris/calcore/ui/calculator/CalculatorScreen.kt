package com.cloveriris.calcore.ui.calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloveriris.calcore.domain.model.CalculatorMode
import com.cloveriris.calcore.presentation.calculator.CalculatorViewModel
import com.cloveriris.calcore.ui.components.CalcoreDisplay
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.visualization.VisualizationStage

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calcore") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isExpanded) {
            // Two-pane layout for tablets and foldables
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Left pane: Calculator
                CalculatorPane(
                    uiState = uiState,
                    onInput = viewModel::onInput,
                    onModeChange = viewModel::onModeChange,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )

                // Right pane: Visualization stage
                VisualizationStage(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
            }
        } else {
            // Single-pane layout for phones
            CalculatorPane(
                uiState = uiState,
                onInput = viewModel::onInput,
                onModeChange = viewModel::onModeChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CalculatorPane(
    uiState: com.cloveriris.calcore.presentation.calculator.CalculatorUiState,
    onInput: (com.cloveriris.calcore.domain.model.CalculatorInput) -> Unit,
    onModeChange: (CalculatorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Mode selector chips
        ModeSelector(
            currentMode = uiState.mode,
            onModeChange = onModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Display
        CalcoreDisplay(
            expression = uiState.state.displayExpression,
            result = uiState.state.displayResult,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Keypad
        StandardKeypad(
            onInput = onInput,
            hasMemory = uiState.memory.hasValue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModeSelector(
    currentMode: CalculatorMode,
    onModeChange: (CalculatorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        CalculatorMode.STANDARD to "Standard",
        CalculatorMode.SCIENTIFIC to "Scientific",
        CalculatorMode.PROGRAMMER to "Programmer",
        CalculatorMode.DATE to "Date"
    )

    Row(
        modifier = modifier
    ) {
        modes.forEachIndexed { index, (mode, label) ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalculatorScreenPhonePreview() {
    CalcoreTheme(appTheme = com.cloveriris.calcore.ui.theme.AppTheme.CALCORE_DARK) {
        CalculatorScreen()
    }
}

@Preview(showBackground = true, widthDp = 840)
@Composable
private fun CalculatorScreenTabletPreview() {
    CalcoreTheme(appTheme = com.cloveriris.calcore.ui.theme.AppTheme.CALCORE_DARK) {
        CalculatorScreen(windowWidthSizeClass = WindowWidthSizeClass.Expanded)
    }
}
