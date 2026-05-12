package com.cloveriris.calcore.ui.graphing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloveriris.calcore.domain.model.graphing.ExpressionType
import com.cloveriris.calcore.domain.model.graphing.GraphExpression
import com.cloveriris.calcore.domain.model.graphing.ViewportState
import com.cloveriris.calcore.domain.model.template.LissajousTemplate
import com.cloveriris.calcore.domain.model.template.MathTemplate
import com.cloveriris.calcore.domain.model.template.ParabolaTemplate
import com.cloveriris.calcore.domain.model.template.TemplateCategory
import com.cloveriris.calcore.domain.model.template.TemplateLibrary
import com.cloveriris.calcore.presentation.graphing.GraphingUiState
import com.cloveriris.calcore.presentation.graphing.GraphingViewModel
import com.cloveriris.calcore.ui.components.CoordinateCanvas
import com.cloveriris.calcore.ui.components.TemplateSelector
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground

val expressionColors = listOf(
    Color(0xFFFF5252),
    Color(0xFF69F0AE),
    Color(0xFF448AFF),
    Color(0xFFFFD740),
    Color(0xFFE040FB),
    Color(0xFF18FFFF),
    Color(0xFFFF6E40),
)

private fun applyTemplate(
    template: MathTemplate,
    viewModel: GraphingViewModel,
    expressionCount: Int,
    onInputCleared: () -> Unit
) {
    onInputCleared()

    val type = when (template) {
        is ParabolaTemplate -> ExpressionType.EXPLICIT_Y
        is LissajousTemplate -> ExpressionType.PARAMETRIC
        else -> ExpressionType.EXPLICIT_Y
    }

    val color = expressionColors[expressionCount % expressionColors.size]
    template.defaultExpressions.firstOrNull()?.let { expr ->
        viewModel.addExpression(expr, color, type)
    }

    template.parameters.forEach { param ->
        viewModel.addParameter(param.name, param.defaultValue)
    }

    template.recommendedViewport?.let { config ->
        val centerX = (config.xRange.start + config.xRange.endInclusive) / 2.0
        val centerY = (config.yRange.start + config.yRange.endInclusive) / 2.0
        viewModel.updateViewport(
            ViewportState(centerX = centerX, centerY = centerY)
        )
    }
}

@Composable
private fun CollapsibleTemplateSection(
    templates: List<MathTemplate>,
    onTemplateSelected: (MathTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "模板库",
                style = MaterialTheme.typography.titleSmall
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        AnimatedVisibility(visible = expanded) {
            TemplateSelector(
                templates = templates,
                onTemplateSelected = onTemplateSelected,
                modifier = Modifier.heightIn(max = 240.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GraphingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var inputText by remember { mutableStateOf("") }
    var inputType by remember { mutableStateOf(ExpressionType.EXPLICIT_Y) }

    val graphingTemplates = remember {
        TemplateLibrary.byCategory(TemplateCategory.GRAPHING)
    }

    val onTemplateSelected: (MathTemplate) -> Unit = { template ->
        applyTemplate(template, viewModel, uiState.expressions.size) { inputText = "" }
        showBottomSheet = false
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth > 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("图形") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* placeholder menu */ }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                BottomInputBar(
                    inputText = inputText,
                    inputType = inputType,
                    onInputChange = { inputText = it },
                    onInputTypeChange = { inputType = it },
                    onAddExpression = {
                        if (inputText.isNotBlank()) {
                            val color = expressionColors[uiState.expressions.size % expressionColors.size]
                            viewModel.addExpression(inputText, color, inputType)
                            inputText = ""
                        }
                    },
                    onResetView = { viewModel.resetViewport() }
                )
            },
            floatingActionButton = {
                if (!isExpanded) {
                    FloatingActionButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Expressions")
                    }
                }
            }
        ) { innerPadding ->
            if (isExpanded) {
                ExpandedLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    templates = graphingTemplates,
                    onTemplateSelected = onTemplateSelected,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                CompactLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        if (!isExpanded && showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    CollapsibleTemplateSection(
                        templates = graphingTemplates,
                        onTemplateSelected = onTemplateSelected
                    )
                    HorizontalDivider()
                    ExpressionListPanel(
                        expressions = uiState.expressions,
                        selectedId = uiState.selectedExpressionId,
                        onSelect = viewModel::selectExpression,
                        onToggleVisibility = viewModel::toggleExpressionVisibility,
                        onRemove = viewModel::removeExpression,
                        modifier = Modifier.weight(1f)
                    )
                    HorizontalDivider()
                    ParameterSliderPanel(
                        parameters = uiState.parameters,
                        onParameterChange = viewModel::updateParameter,
                        onAddParameter = { name, default, range ->
                            viewModel.addParameter(name, default)
                        },
                        onRemoveParameter = viewModel::removeParameter,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedLayout(
    uiState: GraphingUiState,
    viewModel: GraphingViewModel,
    templates: List<MathTemplate>,
    onTemplateSelected: (MathTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Left panel: templates + expressions + parameters
        Column(
            modifier = Modifier
                .widthIn(min = 240.dp, max = 320.dp)
                .weight(0.28f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            CollapsibleTemplateSection(
                templates = templates,
                onTemplateSelected = onTemplateSelected
            )
            HorizontalDivider()
            ExpressionListPanel(
                expressions = uiState.expressions,
                selectedId = uiState.selectedExpressionId,
                onSelect = viewModel::selectExpression,
                onToggleVisibility = viewModel::toggleExpressionVisibility,
                onRemove = viewModel::removeExpression,
                modifier = Modifier.weight(1f)
            )
            HorizontalDivider()
            ParameterSliderPanel(
                parameters = uiState.parameters,
                onParameterChange = viewModel::updateParameter,
                onAddParameter = { name, default, range ->
                    viewModel.addParameter(name, default)
                },
                onRemoveParameter = viewModel::removeParameter,
                modifier = Modifier.weight(1f)
            )
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Center canvas
        CoordinateCanvas(
            viewport = uiState.viewport,
            shapes = uiState.shapes,
            onViewportChange = viewModel::updateViewport,
            onSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Right panel: info + properties
        Column(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .weight(0.22f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "图形属性",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "表达式: ${uiState.expressions.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "参数: ${uiState.parameters.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "视口",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "center: (%.2f, %.2f)".format(
                    uiState.viewport.centerX,
                    uiState.viewport.centerY
                ),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "scale: %.1f px/u".format(uiState.viewport.scale),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.parameters.isNotEmpty()) {
                Text(
                    text = "当前参数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                uiState.parameters.forEach { (name, value) ->
                    Text(
                        text = "$name = %.3f".format(value),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactLayout(
    uiState: GraphingUiState,
    viewModel: GraphingViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        CoordinateCanvas(
            viewport = uiState.viewport,
            shapes = uiState.shapes,
            onViewportChange = viewModel::updateViewport,
            onSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
            modifier = Modifier.fillMaxSize()
        )

        // Floating mini expression bar at top
        if (uiState.expressions.isNotEmpty()) {
            MiniExpressionBar(
                expressions = uiState.expressions,
                selectedId = uiState.selectedExpressionId,
                onSelect = viewModel::selectExpression,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun MiniExpressionBar(
    expressions: List<GraphExpression>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(expressions.size, key = { expressions[it].id }) { index ->
            val expr = expressions[index]
            val isSelected = expr.id == selectedId
            Surface(
                onClick = { onSelect(expr.id) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) expr.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(expr.color, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                        text = expr.rawExpression.take(18),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomInputBar(
    inputText: String,
    inputType: ExpressionType,
    onInputChange: (String) -> Unit,
    onInputTypeChange: (ExpressionType) -> Unit,
    onAddExpression: () -> Unit,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val placeholderText = when (inputType) {
        ExpressionType.EXPLICIT_Y -> "Enter expression, e.g. x^2 + 1"
        ExpressionType.POLAR -> "Enter r = f(θ), e.g. 1 + sin(theta)"
        ExpressionType.PARAMETRIC -> "Enter x(t), y(t), e.g. cos(t), sin(t)"
        else -> "Enter expression"
    }

    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .padding(bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                SegmentedButton(
                    selected = inputType == ExpressionType.EXPLICIT_Y,
                    onClick = { onInputTypeChange(ExpressionType.EXPLICIT_Y) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("y=", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = inputType == ExpressionType.POLAR,
                    onClick = { onInputTypeChange(ExpressionType.POLAR) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("r=", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = inputType == ExpressionType.PARAMETRIC,
                    onClick = { onInputTypeChange(ExpressionType.PARAMETRIC) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("param", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text(placeholderText) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,

                )
                IconButton(onClick = onAddExpression) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                TextButton(onClick = onResetView) {
                    Text("Reset View")
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun GraphingScreenExpandedPreview() {
    CalcoreTheme {
        GraphingScreen()
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun GraphingScreenCompactPreview() {
    CalcoreTheme {
        GraphingScreen()
    }
}
