package com.cloveriris.calcore.presentation.graphing

import androidx.compose.ui.graphics.Color
import com.cloveriris.calcore.domain.model.graphing.GraphExpression
import com.cloveriris.calcore.domain.model.graphing.GraphShape
import com.cloveriris.calcore.domain.model.graphing.ViewportState

data class GraphingUiState(
    val expressions: List<GraphExpression> = emptyList(),
    val parameters: Map<String, Double> = emptyMap(),
    val viewport: ViewportState = ViewportState(),
    val shapes: List<GraphShape> = emptyList(),
    val isLoading: Boolean = false,
    val selectedExpressionId: String? = null,
    val errorMessage: String? = null
)
