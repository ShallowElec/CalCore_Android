package com.cloveriris.calcore.ui.diffeq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloveriris.calcore.domain.model.equation.OdeSolution
import com.cloveriris.calcore.domain.model.equation.SolverMethod
import com.cloveriris.calcore.presentation.diffeq.DiffEqViewModel
import com.cloveriris.calcore.presentation.diffeq.OdeTemplate
import com.cloveriris.calcore.ui.theme.TerminalBackground
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifferentialEquationsScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DiffEqViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val template = viewModel.templates.getOrNull(uiState.selectedTemplateIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("微分方程工作台", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight && maxWidth > 600.dp

            if (isLandscape) {
                LandscapeLayout(
                    uiState = uiState,
                    template = template,
                    templates = viewModel.templates,
                    viewModel = viewModel
                )
            } else {
                PortraitLayout(
                    uiState = uiState,
                    template = template,
                    templates = viewModel.templates,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: com.cloveriris.calcore.presentation.diffeq.DiffEqUiState,
    template: OdeTemplate?,
    templates: List<OdeTemplate>,
    viewModel: DiffEqViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TemplateSelector(
            templates = templates,
            selectedIndex = uiState.selectedTemplateIndex,
            solverMethod = uiState.solverMethod,
            onTemplateSelected = viewModel::selectTemplate,
            onSolverChanged = viewModel::setSolverMethod
        )

        HorizontalDivider()

        DiffEqCanvas(
            solution = uiState.solution,
            template = template,
            currentTimeIndex = uiState.currentTimeIndex,
            showDirectionField = uiState.showDirectionField,
            showPhasePortrait = uiState.showPhasePortrait,
            viewport = uiState.viewport,
            onPan = { dx, dy -> viewModel.panViewport(dx, dy) },
            onZoom = { factor, ax, ay -> viewModel.zoomViewport(factor, ax, ay) },
            onReset = viewModel::resetViewport,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(TerminalBackground)
        )

        BottomControlSection(
            uiState = uiState,
            template = template,
            onParameterChange = viewModel::setParameter,
            onTimeEndChange = viewModel::setTimeEnd,
            onTimeStepChange = viewModel::setTimeStep,
            onPlayPause = viewModel::playPause,
            onScrub = { progress ->
                val sol = uiState.solution
                if (sol != null && sol.timePoints.isNotEmpty()) {
                    val index = (progress * sol.timePoints.lastIndex).toInt()
                    viewModel.setTimeIndex(index)
                }
            },
            onToggleDirectionField = viewModel::toggleDirectionField,
            onTogglePhasePortrait = viewModel::togglePhasePortrait
        )
    }
}

@Composable
private fun LandscapeLayout(
    uiState: com.cloveriris.calcore.presentation.diffeq.DiffEqUiState,
    template: OdeTemplate?,
    templates: List<OdeTemplate>,
    viewModel: DiffEqViewModel,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 左侧：模板 + 求解器 + 参数
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            TemplateSelector(
                templates = templates,
                selectedIndex = uiState.selectedTemplateIndex,
                solverMethod = uiState.solverMethod,
                onTemplateSelected = viewModel::selectTemplate,
                onSolverChanged = viewModel::setSolverMethod
            )
            Spacer(modifier = Modifier.height(8.dp))
            ParameterPanel(
                uiState = uiState,
                template = template,
                onParameterChange = viewModel::setParameter,
                onTimeEndChange = viewModel::setTimeEnd,
                onTimeStepChange = viewModel::setTimeStep,
                onToggleDirectionField = viewModel::toggleDirectionField,
                onTogglePhasePortrait = viewModel::togglePhasePortrait
            )
        }

        VerticalDivider()

        // 中央 Canvas
        DiffEqCanvas(
            solution = uiState.solution,
            template = template,
            currentTimeIndex = uiState.currentTimeIndex,
            showDirectionField = uiState.showDirectionField,
            showPhasePortrait = uiState.showPhasePortrait,
            viewport = uiState.viewport,
            onPan = { dx, dy -> viewModel.panViewport(dx, dy) },
            onZoom = { factor, ax, ay -> viewModel.zoomViewport(factor, ax, ay) },
            onReset = viewModel::resetViewport,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(TerminalBackground)
        )

        VerticalDivider()

        // 右侧：播放控制
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            PlaybackControls(
                uiState = uiState,
                onPlayPause = viewModel::playPause,
                onScrub = { progress ->
                    val sol = uiState.solution
                    if (sol != null && sol.timePoints.isNotEmpty()) {
                        val index = (progress * sol.timePoints.lastIndex).toInt()
                        viewModel.setTimeIndex(index)
                    }
                },
                onReset = viewModel::resetViewport
            )
        }
    }
}

@Composable
private fun TemplateSelector(
    templates: List<OdeTemplate>,
    selectedIndex: Int,
    solverMethod: SolverMethod,
    onTemplateSelected: (Int) -> Unit,
    onSolverChanged: (SolverMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            templates.forEachIndexed { index, template ->
                SegmentedButton(
                    selected = selectedIndex == index,
                    onClick = { onTemplateSelected(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = templates.size)
                ) {
                    Text(template.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("求解器:", style = MaterialTheme.typography.labelSmall)
            SolverMethod.entries.forEach { method ->
                FilterChip(
                    selected = solverMethod == method,
                    onClick = { onSolverChanged(method) },
                    label = {
                        Text(
                            when (method) {
                                SolverMethod.EULER -> "Euler"
                                SolverMethod.EULER_IMPROVED -> "Heun"
                                SolverMethod.RK4 -> "RK4"
                                SolverMethod.RKF45 -> "RKF45"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DiffEqCanvas(
    solution: OdeSolution?,
    template: OdeTemplate?,
    currentTimeIndex: Int,
    showDirectionField: Boolean,
    showPhasePortrait: Boolean,
    viewport: com.cloveriris.calcore.presentation.diffeq.DiffEqViewport,
    onPan: (Double, Double) -> Unit,
    onZoom: (Double, Double, Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val rangeX = viewport.maxX - viewport.minX
                    val rangeY = viewport.maxY - viewport.minY

                    val dxWorld = pan.x / w * rangeX
                    val dyWorld = -pan.y / h * rangeY
                    onPan(dxWorld.toDouble(), dyWorld.toDouble())

                    val cx = viewport.minX + (centroid.x / w) * rangeX
                    val cy = viewport.minY + ((h - centroid.y) / h) * rangeY
                    onZoom(zoom.toDouble(), cx, cy)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onReset() })
            }
    ) {
        val w = size.width
        val h = size.height

        drawRect(color = Color(0xFF0A0A0A))

        val worldToScreen: (Double, Double) -> Offset = { wx, wy ->
            val sx = ((wx - viewport.minX) / (viewport.maxX - viewport.minX) * w).toFloat()
            val sy = ((viewport.maxY - wy) / (viewport.maxY - viewport.minY) * h).toFloat()
            Offset(sx, sy)
        }

        drawGrid(worldToScreen, viewport, w, h, textMeasurer)

        if (solution == null || template == null) return@Canvas

        val is2D = template.variableCount == 2

        if (is2D && showPhasePortrait) {
            drawPhasePortrait(solution, worldToScreen, currentTimeIndex)
        } else {
            drawTimeSeries(solution, worldToScreen, currentTimeIndex, template.variableNames)
        }

        if (showDirectionField && !is2D) {
            drawDirectionField1D(template, viewport, worldToScreen)
        } else if (showDirectionField && is2D) {
            drawDirectionField2D(template, viewport, worldToScreen)
        }
    }
}

private fun DrawScope.drawGrid(
    worldToScreen: (Double, Double) -> Offset,
    viewport: com.cloveriris.calcore.presentation.diffeq.DiffEqViewport,
    w: Float, h: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val gridColor = Color(0xFF1F1F1F)
    val axisColor = Color(0xFF8B949E)

    val xStep = calculateStep(viewport.maxX - viewport.minX)
    var x = (viewport.minX / xStep).toInt() * xStep
    while (x <= viewport.maxX) {
        val s = worldToScreen(x, viewport.minY)
        val e = worldToScreen(x, viewport.maxY)
        drawLine(gridColor, s, e, strokeWidth = 1f)
        x += xStep
    }

    val yStep = calculateStep(viewport.maxY - viewport.minY)
    var y = (viewport.minY / yStep).toInt() * yStep
    while (y <= viewport.maxY) {
        val s = worldToScreen(viewport.minX, y)
        val e = worldToScreen(viewport.maxX, y)
        drawLine(gridColor, s, e, strokeWidth = 1f)
        y += yStep
    }

    val ox = worldToScreen(0.0, 0.0)
    if (ox.x in 0f..w) drawLine(axisColor, Offset(ox.x, 0f), Offset(ox.x, h), strokeWidth = 1.5f)
    if (ox.y in 0f..h) drawLine(axisColor, Offset(0f, ox.y), Offset(w, ox.y), strokeWidth = 1.5f)
}

private fun DrawScope.drawTimeSeries(
    solution: OdeSolution,
    worldToScreen: (Double, Double) -> Offset,
    currentTimeIndex: Int,
    variableNames: List<String>
) {
    val colors = listOf(0xFF00FF41, 0xFF448AFF, 0xFFFF5252, 0xFFFFD740).map { Color(it) }
    val endIndex = currentTimeIndex.coerceAtMost(solution.timePoints.lastIndex)

    for (varIdx in solution.values.first().indices) {
        val points = (0..endIndex).map { i ->
            worldToScreen(solution.timePoints[i], solution.values[i][varIdx])
        }
        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(path, colors[varIdx % colors.size], style = Stroke(width = 2.5f))
        }
        if (endIndex >= 0) {
            val curr = worldToScreen(solution.timePoints[endIndex], solution.values[endIndex][varIdx])
            drawCircle(colors[varIdx % colors.size], radius = 5f, center = curr)
        }
    }
}

private fun DrawScope.drawPhasePortrait(
    solution: OdeSolution,
    worldToScreen: (Double, Double) -> Offset,
    currentTimeIndex: Int
) {
    if (solution.values.first().size < 2) return
    val endIndex = currentTimeIndex.coerceAtMost(solution.timePoints.lastIndex)

    val points = (0..endIndex).map { i ->
        worldToScreen(solution.values[i][0], solution.values[i][1])
    }
    if (points.size >= 2) {
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(path, Color(0xFF00FF41), style = Stroke(width = 2.5f))
    }
    if (endIndex >= 0) {
        val curr = worldToScreen(solution.values[endIndex][0], solution.values[endIndex][1])
        drawCircle(Color(0xFF00FF41), radius = 6f, center = curr)
    }
}

private fun DrawScope.drawDirectionField1D(
    template: OdeTemplate,
    viewport: com.cloveriris.calcore.presentation.diffeq.DiffEqViewport,
    worldToScreen: (Double, Double) -> Offset
) {
    val params = template.defaultParameters
    val nx = 15
    val ny = 10
    for (ix in 0..nx) {
        for (iy in 0..ny) {
            val t = viewport.minX + ix * (viewport.maxX - viewport.minX) / nx
            val y = viewport.minY + iy * (viewport.maxY - viewport.minY) / ny
            val slope = template.dydt(t, listOf(y), params)[0]
            val angle = kotlin.math.atan(slope)
            val len = 8f
            val center = worldToScreen(t, y)
            val dx = (len * kotlin.math.cos(angle)).toFloat()
            val dy = (-len * kotlin.math.sin(angle)).toFloat()
            drawLine(
                color = Color(0x558B949E),
                start = Offset(center.x - dx, center.y - dy),
                end = Offset(center.x + dx, center.y + dy),
                strokeWidth = 1f
            )
        }
    }
}

private fun DrawScope.drawDirectionField2D(
    template: OdeTemplate,
    viewport: com.cloveriris.calcore.presentation.diffeq.DiffEqViewport,
    worldToScreen: (Double, Double) -> Offset
) {
    val params = template.defaultParameters
    val nx = 12
    val ny = 10
    for (ix in 0..nx) {
        for (iy in 0..ny) {
            val x = viewport.minX + ix * (viewport.maxX - viewport.minX) / nx
            val y = viewport.minY + iy * (viewport.maxY - viewport.minY) / ny
            val d = template.dydt(0.0, listOf(x, y), params)
            val mag = kotlin.math.hypot(d[0], d[1])
            if (mag > 1e-6) {
                val scale = 0.15 / mag
                val center = worldToScreen(x, y)
                val dx = (d[0] * scale * (viewport.maxX - viewport.minX)).toFloat()
                val dy = (-(d[1] * scale * (viewport.maxY - viewport.minY))).toFloat()
                drawLine(
                    color = Color(0x558B949E),
                    start = center,
                    end = Offset(center.x + dx, center.y + dy),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private fun calculateStep(range: Double): Double {
    val rough = range / 8.0
    val exp = kotlin.math.floor(kotlin.math.log10(rough))
    val frac = rough / 10.0.pow(exp)
    val nice = if (frac < 1.5) 1.0 else if (frac < 3.0) 2.0 else if (frac < 7.0) 5.0 else 10.0
    return nice * 10.0.pow(exp)
}

@Composable
private fun BottomControlSection(
    uiState: com.cloveriris.calcore.presentation.diffeq.DiffEqUiState,
    template: OdeTemplate?,
    onParameterChange: (String, Double) -> Unit,
    onTimeEndChange: (Double) -> Unit,
    onTimeStepChange: (Double) -> Unit,
    onPlayPause: () -> Unit,
    onScrub: (Float) -> Unit,
    onToggleDirectionField: () -> Unit,
    onTogglePhasePortrait: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        PlaybackControls(
            uiState = uiState,
            onPlayPause = onPlayPause,
            onScrub = onScrub,
            onReset = {}
        )

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        ParameterPanel(
            uiState = uiState,
            template = template,
            onParameterChange = onParameterChange,
            onTimeEndChange = onTimeEndChange,
            onTimeStepChange = onTimeStepChange,
            onToggleDirectionField = onToggleDirectionField,
            onTogglePhasePortrait = onTogglePhasePortrait
        )
    }
}

@Composable
private fun PlaybackControls(
    uiState: com.cloveriris.calcore.presentation.diffeq.DiffEqUiState,
    onPlayPause: () -> Unit,
    onScrub: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (uiState.isPlaying) "暂停" else "播放"
            )
        }
        IconButton(onClick = onReset, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "重置视图")
        }
        val progress = if (uiState.solution != null && uiState.solution.timePoints.isNotEmpty()) {
            uiState.currentTimeIndex / uiState.solution.timePoints.lastIndex.toFloat()
        } else 0f
        Slider(
            value = progress,
            onValueChange = onScrub,
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f
        )
        Text(
            text = uiState.solution?.timePoints?.getOrNull(uiState.currentTimeIndex)?.let { "t=%.2f".format(it) } ?: "t=0.00",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ParameterPanel(
    uiState: com.cloveriris.calcore.presentation.diffeq.DiffEqUiState,
    template: OdeTemplate?,
    onParameterChange: (String, Double) -> Unit,
    onTimeEndChange: (Double) -> Unit,
    onTimeStepChange: (Double) -> Unit,
    onToggleDirectionField: () -> Unit,
    onTogglePhasePortrait: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .verticalScroll(rememberScrollState())
    ) {
        template?.defaultParameters?.keys?.forEach { paramName ->
            val value = uiState.parameters[paramName] ?: 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$paramName = %.3f".format(value),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(90.dp)
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onParameterChange(paramName, it.toDouble()) },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.01f..5f
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("t_end:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
            Slider(
                value = uiState.timeEnd.toFloat(),
                onValueChange = { onTimeEndChange(it.toDouble()) },
                modifier = Modifier.weight(1f),
                valueRange = 1f..50f
            )
            Text("%.1f".format(uiState.timeEnd), style = MaterialTheme.typography.labelSmall)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = uiState.showDirectionField,
                onClick = onToggleDirectionField,
                label = { Text("方向场", style = MaterialTheme.typography.labelSmall) }
            )
            if (template?.variableCount == 2) {
                FilterChip(
                    selected = uiState.showPhasePortrait,
                    onClick = onTogglePhasePortrait,
                    label = { Text("相图", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
