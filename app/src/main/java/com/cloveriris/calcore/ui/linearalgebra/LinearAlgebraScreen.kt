package com.cloveriris.calcore.ui.linearalgebra

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloveriris.calcore.domain.model.matrix.Matrix
import com.cloveriris.calcore.presentation.linearalgebra.LinearAlgebraUiState
import com.cloveriris.calcore.presentation.linearalgebra.LinearAlgebraViewModel
import com.cloveriris.calcore.presentation.linearalgebra.MatrixOperation
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalGray
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearAlgebraScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LinearAlgebraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-dismiss error after 2 seconds
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(2000)
            viewModel.dismissError()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isExpanded = screenWidth > 600.dp
        val isLandscape = screenWidth > screenHeight

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("线性代数工作台", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isExpanded && isLandscape) {
                        ExpandedLayout(
                            uiState = uiState,
                            viewModel = viewModel,
                            availableWidth = screenWidth,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        CompactLayout(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Step animation control bar
                    if (uiState.steps.isNotEmpty()) {
                        StepControlBar(
                            isAnimating = uiState.isAnimating,
                            progress = uiState.animationProgress,
                            onPlayPause = {
                                if (uiState.isAnimating) viewModel.pauseAnimation()
                                else viewModel.startStepAnimation()
                            },
                            onReset = viewModel::resetAnimation
                        )
                    }
                }

                // Error overlay
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (uiState.steps.isNotEmpty()) 56.dp else 16.dp
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedLayout(
    uiState: LinearAlgebraUiState,
    viewModel: LinearAlgebraViewModel,
    availableWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    // Responsive panel widths: shrink on medium screens, expand on large tablets
    val leftWidth = if (availableWidth > 1000.dp) 260.dp else if (availableWidth > 800.dp) 220.dp else 200.dp
    val rightWidth = if (availableWidth > 1000.dp) 280.dp else if (availableWidth > 800.dp) 240.dp else 200.dp

    Row(modifier = modifier.fillMaxSize()) {
        // Left panel: Matrix list + Operation toolbar
        Column(
            modifier = Modifier
                .width(leftWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            MatrixListPanel(
                matrices = uiState.matrices,
                selectedIndex = uiState.selectedMatrixIndex,
                onSelect = viewModel::selectMatrix,
                onAddMatrix = { viewModel.addMatrix() },
                onRemoveMatrix = viewModel::removeMatrix,
                modifier = Modifier.weight(1f)
            )
            HorizontalDivider()
            OperationToolbar(
                matrices = uiState.matrices,
                selectedIndex = uiState.selectedMatrixIndex,
                secondaryIndex = uiState.secondarySelectedIndex,
                onSelectSecondary = viewModel::selectSecondaryMatrix,
                onOperation = viewModel::performOperation,
                modifier = Modifier.heightIn(min = 160.dp, max = 260.dp)
            )
        }

        VerticalDivider(modifier = Modifier.fillMaxHeight())

        // Center: Matrix editor or vector transform canvas
        Box(modifier = Modifier.weight(1f)) {
            val matrix = uiState.matrices.getOrNull(uiState.selectedMatrixIndex)
            if (uiState.showVectorTransform && uiState.vectorTransformMatrix != null) {
                VectorTransformCanvas(
                    matrix = uiState.vectorTransformMatrix,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (matrix != null) {
                MatrixEditor(
                    matrix = matrix,
                    onCellChange = { row, col, value ->
                        viewModel.updateCell(uiState.selectedMatrixIndex, row, col, value)
                    },
                    onAddRow = { viewModel.addRow(uiState.selectedMatrixIndex) },
                    onRemoveRow = { viewModel.removeRow(uiState.selectedMatrixIndex) },
                    onAddCol = { viewModel.addCol(uiState.selectedMatrixIndex) },
                    onRemoveCol = { viewModel.removeCol(uiState.selectedMatrixIndex) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                EmptyState(message = "请选择一个矩阵")
            }
        }

        VerticalDivider(modifier = Modifier.fillMaxHeight())

        // Right panel: Result panel
        ResultPanel(
            result = uiState.result,
            scalarResult = uiState.scalarResult,
            operation = uiState.operation,
            steps = uiState.steps,
            currentStepIndex = uiState.currentStepIndex,
            isAnimating = uiState.isAnimating,
            onPlayPause = {
                if (uiState.isAnimating) viewModel.pauseAnimation()
                else viewModel.startStepAnimation()
            },
            onReset = viewModel::resetAnimation,
            modifier = Modifier
                .width(rightWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactLayout(
    uiState: LinearAlgebraUiState,
    viewModel: LinearAlgebraViewModel,
    modifier: Modifier = Modifier
) {
    var showResultSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(modifier = modifier.fillMaxSize()) {
        // Top: horizontal matrix list
        MatrixListPanelCompact(
            matrices = uiState.matrices,
            selectedIndex = uiState.selectedMatrixIndex,
            onSelect = viewModel::selectMatrix,
            onAddMatrix = { viewModel.addMatrix() },
            onRemoveMatrix = viewModel::removeMatrix,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        HorizontalDivider()

        // Center: Matrix editor or vector transform canvas
        Box(modifier = Modifier.weight(1f)) {
            val matrix = uiState.matrices.getOrNull(uiState.selectedMatrixIndex)
            if (uiState.showVectorTransform && uiState.vectorTransformMatrix != null) {
                VectorTransformCanvas(
                    matrix = uiState.vectorTransformMatrix,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (matrix != null) {
                MatrixEditor(
                    matrix = matrix,
                    onCellChange = { row, col, value ->
                        viewModel.updateCell(uiState.selectedMatrixIndex, row, col, value)
                    },
                    onAddRow = { viewModel.addRow(uiState.selectedMatrixIndex) },
                    onRemoveRow = { viewModel.removeRow(uiState.selectedMatrixIndex) },
                    onAddCol = { viewModel.addCol(uiState.selectedMatrixIndex) },
                    onRemoveCol = { viewModel.removeCol(uiState.selectedMatrixIndex) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                EmptyState(message = "请选择一个矩阵")
            }
        }

        HorizontalDivider()

        // Bottom: Operation toolbar
        OperationToolbar(
            matrices = uiState.matrices,
            selectedIndex = uiState.selectedMatrixIndex,
            secondaryIndex = uiState.secondarySelectedIndex,
            onSelectSecondary = viewModel::selectSecondaryMatrix,
            onOperation = { op ->
                viewModel.performOperation(op)
                if (op != MatrixOperation.NONE) {
                    showResultSheet = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 160.dp)
        )
    }

    if (showResultSheet) {
        ModalBottomSheet(
            onDismissRequest = { showResultSheet = false },
            sheetState = sheetState
        ) {
            ResultPanel(
                result = uiState.result,
                scalarResult = uiState.scalarResult,
                operation = uiState.operation,
                steps = uiState.steps,
                currentStepIndex = uiState.currentStepIndex,
                isAnimating = uiState.isAnimating,
                onPlayPause = {
                    if (uiState.isAnimating) viewModel.pauseAnimation()
                    else viewModel.startStepAnimation()
                },
                onReset = viewModel::resetAnimation,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
            )
        }
    }
}

@Composable
private fun MatrixListPanelCompact(
    matrices: List<Matrix<Double>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAddMatrix: () -> Unit,
    onRemoveMatrix: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(count = matrices.size) { index ->
            val matrix = matrices[index]
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier.clickable { onSelect(index) },
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column {
                        Text(matrix.name, style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${matrix.rows}×${matrix.cols}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(
                        onClick = { onRemoveMatrix(index) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "删除",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        item {
            IconButton(onClick = onAddMatrix) {
                Icon(Icons.Default.Add, contentDescription = "添加矩阵")
            }
        }
    }
}

@Composable
private fun StepControlBar(
    isAnimating: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isAnimating) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isAnimating) "暂停" else "播放",
                    modifier = Modifier.size(20.dp)
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )
            IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "重置", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TerminalGray
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun LinearAlgebraScreenExpandedPreview() {
    CalcoreTheme {
        LinearAlgebraScreen()
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun LinearAlgebraScreenCompactPreview() {
    CalcoreTheme {
        LinearAlgebraScreen()
    }
}
