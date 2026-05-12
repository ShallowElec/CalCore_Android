package com.cloveriris.calcore.ui.calculus

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.geometry.Size
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
import com.cloveriris.calcore.domain.model.calculus.*
import com.cloveriris.calcore.presentation.calculus.CalculusMode
import com.cloveriris.calcore.presentation.calculus.CalculusUiState
import com.cloveriris.calcore.presentation.calculus.CalculusViewModel
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import com.cloveriris.calcore.ui.theme.TerminalBackground
import com.cloveriris.calcore.ui.theme.TerminalGray
import com.cloveriris.calcore.ui.theme.TerminalGreen
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculusScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalculusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("微积分可视化", style = MaterialTheme.typography.titleMedium) },
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
                LandscapeLayout(uiState = uiState, viewModel = viewModel)
            } else {
                PortraitLayout(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: CalculusUiState,
    viewModel: CalculusViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 紧凑模式切换栏
        ModeSelector(
            currentMode = uiState.mode,
            onModeSelected = viewModel::setMode
        )

        // 紧凑参数输入
        ParameterInputBar(
            uiState = uiState,
            onFunctionChange = viewModel::setFunction,
            onPointChange = viewModel::setPoint,
            onBoundsChange = viewModel::setBounds,
            onCenterChange = viewModel::setCenter,
            onOrderChange = viewModel::setOrder,
            onSubdivisionsChange = viewModel::setSubdivisions,
            onMethodChange = viewModel::setIntegrationMethod
        )

        HorizontalDivider()

        // Canvas（占最大空间）
        CalculusCanvas(
            frame = uiState.frames.getOrNull(uiState.currentFrameIndex),
            viewport = uiState.viewport,
            onPan = { dx, dy -> viewModel.panViewport(dx, dy) },
            onZoom = { factor, ax, ay -> viewModel.zoomViewport(factor, ax, ay) },
            onReset = viewModel::resetView,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(TerminalBackground)
        )

        // 底部控制
        BottomControlSection(
            uiState = uiState,
            onPlayPause = viewModel::playPause,
            onScrub = { progress ->
                val index = (progress * uiState.frames.lastIndex).toInt()
                viewModel.setFrameIndex(index.coerceAtLeast(0))
            },
            onReset = viewModel::resetView
        )
    }
}

@Composable
private fun LandscapeLayout(
    uiState: CalculusUiState,
    viewModel: CalculusViewModel,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 左侧面板：模式 + 参数
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            ModeSelector(
                currentMode = uiState.mode,
                onModeSelected = viewModel::setMode
            )
            Spacer(modifier = Modifier.height(8.dp))
            ParameterInputBar(
                uiState = uiState,
                onFunctionChange = viewModel::setFunction,
                onPointChange = viewModel::setPoint,
                onBoundsChange = viewModel::setBounds,
                onCenterChange = viewModel::setCenter,
                onOrderChange = viewModel::setOrder,
                onSubdivisionsChange = viewModel::setSubdivisions,
                onMethodChange = viewModel::setIntegrationMethod
            )
        }

        VerticalDivider()

        // 中央 Canvas
        CalculusCanvas(
            frame = uiState.frames.getOrNull(uiState.currentFrameIndex),
            viewport = uiState.viewport,
            onPan = { dx, dy -> viewModel.panViewport(dx, dy) },
            onZoom = { factor, ax, ay -> viewModel.zoomViewport(factor, ax, ay) },
            onReset = viewModel::resetView,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(TerminalBackground)
        )

        VerticalDivider()

        // 右侧：播放控制 + 步骤
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            BottomControlSection(
                uiState = uiState,
                onPlayPause = viewModel::playPause,
                onScrub = { progress ->
                    val index = (progress * uiState.frames.lastIndex).toInt()
                    viewModel.setFrameIndex(index.coerceAtLeast(0))
                },
                onReset = viewModel::resetView
            )
        }
    }
}

@Composable
private fun ModeSelector(
    currentMode: CalculusMode,
    onModeSelected: (CalculusMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        CalculusMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = CalculusMode.entries.size)
            ) {
                Text(mode.displayName, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ParameterInputBar(
    uiState: CalculusUiState,
    onFunctionChange: (String) -> Unit,
    onPointChange: (Double) -> Unit,
    onBoundsChange: (Double, Double) -> Unit,
    onCenterChange: (Double) -> Unit,
    onOrderChange: (Int) -> Unit,
    onSubdivisionsChange: (Int) -> Unit,
    onMethodChange: (IntegrationMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = uiState.function,
                onValueChange = onFunctionChange,
                label = { Text("f(x)", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )

            when (uiState.mode) {
                CalculusMode.LIMIT, CalculusMode.DERIVATIVE -> {
                    OutlinedTextField(
                        value = uiState.point.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let(onPointChange) },
                        label = { Text("x₀", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.width(72.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
                CalculusMode.INTEGRAL -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = uiState.lowerBound.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { v -> onBoundsChange(v, uiState.upperBound) } },
                            label = { Text("a", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            modifier = Modifier.width(56.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = uiState.upperBound.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { v -> onBoundsChange(uiState.lowerBound, v) } },
                            label = { Text("b", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            modifier = Modifier.width(56.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                CalculusMode.TAYLOR -> {
                    OutlinedTextField(
                        value = uiState.center.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let(onCenterChange) },
                        label = { Text("a", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.width(72.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculusCanvas(
    frame: GeometryFrame?,
    viewport: com.cloveriris.calcore.presentation.calculus.CalculusViewport,
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

                    // 平移：屏幕像素 → 世界坐标
                    val dxWorld = pan.x / w * rangeX
                    val dyWorld = -pan.y / h * rangeY
                    onPan(dxWorld.toDouble(), dyWorld.toDouble())

                    // 缩放：以 centroid 为锚点
                    val cx = viewport.minX + (centroid.x / w) * rangeX
                    val cy = viewport.minY + ((h - centroid.y) / h) * rangeY
                    onZoom(zoom.toDouble(), cx, cy)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onReset() })
            }
    ) {
        val width = size.width
        val height = size.height

        drawRect(color = Color(0xFF0A0A0A))

        val worldToScreen: (Double, Double) -> Offset = { wx, wy ->
            val sx = ((wx - viewport.minX) / (viewport.maxX - viewport.minX) * width).toFloat()
            val sy = ((viewport.maxY - wy) / (viewport.maxY - viewport.minY) * height).toFloat()
            Offset(sx, sy)
        }

        drawGrid(worldToScreen, viewport, width, height, textMeasurer)

        if (frame != null) {
            frame.shapes.forEach { shape ->
                drawShape(shape, worldToScreen, textMeasurer)
            }
            frame.labels.forEach { label ->
                val pos = worldToScreen(label.x, label.y)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label.text,
                    topLeft = pos,
                    style = TextStyle(
                        color = Color(label.color),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawGrid(
    worldToScreen: (Double, Double) -> Offset,
    viewport: com.cloveriris.calcore.presentation.calculus.CalculusViewport,
    width: Float, height: Float,
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
        if (kotlin.math.abs(x) > 0.001) {
            drawText(
                textMeasurer = textMeasurer,
                text = "%.1f".format(x),
                topLeft = worldToScreen(x, viewport.minY) + Offset(2f, 2f),
                style = TextStyle(color = axisColor.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            )
        }
        x += xStep
    }

    val yStep = calculateStep(viewport.maxY - viewport.minY)
    var y = (viewport.minY / yStep).toInt() * yStep
    while (y <= viewport.maxY) {
        val s = worldToScreen(viewport.minX, y)
        val e = worldToScreen(viewport.maxX, y)
        drawLine(gridColor, s, e, strokeWidth = 1f)
        if (kotlin.math.abs(y) > 0.001) {
            drawText(
                textMeasurer = textMeasurer,
                text = "%.1f".format(y),
                topLeft = worldToScreen(viewport.minX, y) + Offset(4f, -10f),
                style = TextStyle(color = axisColor.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            )
        }
        y += yStep
    }

    val ox = worldToScreen(0.0, 0.0)
    if (ox.x in 0f..width) {
        drawLine(axisColor, Offset(ox.x, 0f), Offset(ox.x, height), strokeWidth = 1.5f)
    }
    if (ox.y in 0f..height) {
        drawLine(axisColor, Offset(0f, ox.y), Offset(width, ox.y), strokeWidth = 1.5f)
    }
}

private fun calculateStep(range: Double): Double {
    val rough = range / 8.0
    val exp = kotlin.math.floor(kotlin.math.log10(rough))
    val frac = rough / 10.0.pow(exp)
    val nice = if (frac < 1.5) 1.0 else if (frac < 3.0) 2.0 else if (frac < 7.0) 5.0 else 10.0
    return nice * 10.0.pow(exp)
}

private fun DrawScope.drawShape(
    shape: DrawableShape,
    worldToScreen: (Double, Double) -> Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    when (shape) {
        is DrawableShape.Point -> {
            val center = worldToScreen(shape.x, shape.y)
            drawCircle(color = Color(shape.color), radius = shape.radius, center = center)
        }
        is DrawableShape.Line -> {
            val start = worldToScreen(shape.x1, shape.y1)
            val end = worldToScreen(shape.x2, shape.y2)
            if (shape.isDashed) {
                drawLine(color = Color(shape.color), start = start, end = end, strokeWidth = shape.strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f))
            } else {
                drawLine(color = Color(shape.color), start = start, end = end, strokeWidth = shape.strokeWidth)
            }
        }
        is DrawableShape.Rect -> {
            val topLeft = worldToScreen(shape.x, shape.y + shape.height)
            val bottomRight = worldToScreen(shape.x + shape.width, shape.y)
            val size = Size((bottomRight.x - topLeft.x).coerceAtLeast(1f), (bottomRight.y - topLeft.y).coerceAtLeast(1f))
            drawRect(color = Color(shape.fillColor), topLeft = topLeft, size = size)
            drawRect(color = Color(shape.strokeColor), topLeft = topLeft, size = size, style = Stroke(width = 1f))
        }
        is DrawableShape.Circle -> {
            val center = worldToScreen(shape.cx, shape.cy)
            val edge = worldToScreen(shape.cx + shape.radius, shape.cy)
            val r = (edge.x - center.x).coerceAtLeast(1f)
            shape.fillColor?.let { drawCircle(Color(it), radius = r, center = center) }
            drawCircle(color = Color(shape.strokeColor), radius = r, center = center, style = Stroke(width = shape.strokeWidth))
        }
        is DrawableShape.Path -> {
            if (shape.points.size >= 2) {
                val path = Path().apply {
                    val first = worldToScreen(shape.points[0].first, shape.points[0].second)
                    moveTo(first.x, first.y)
                    for (i in 1 until shape.points.size) {
                        val p = worldToScreen(shape.points[i].first, shape.points[i].second)
                        lineTo(p.x, p.y)
                    }
                }
                drawPath(path = path, color = Color(shape.color), style = Stroke(width = shape.strokeWidth))
            }
        }
        is DrawableShape.Arrow -> {
            val start = worldToScreen(shape.x1, shape.y1)
            val end = worldToScreen(shape.x2, shape.y2)
            drawLine(Color(shape.color), start, end, strokeWidth = 2f)
        }
    }
}

@Composable
private fun BottomControlSection(
    uiState: CalculusUiState,
    onPlayPause: () -> Unit,
    onScrub: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                Icon(Icons.Default.Refresh, contentDescription = "重置")
            }
            val progress = if (uiState.frames.isNotEmpty()) {
                uiState.currentFrameIndex / uiState.frames.lastIndex.toFloat()
            } else 0f
            Slider(
                value = progress,
                onValueChange = onScrub,
                modifier = Modifier.weight(1f),
                valueRange = 0f..1f
            )
            Text(
                text = "${uiState.currentFrameIndex + 1}/${uiState.frames.size}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
        }

        if (uiState.steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                uiState.steps.forEach { step ->
                    StepCard(step = step)
                }
            }
        }
    }
}

@Composable
private fun StepCard(step: CalculusStep, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(vertical = 1.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(text = step.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(text = step.formula, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text(text = step.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}
