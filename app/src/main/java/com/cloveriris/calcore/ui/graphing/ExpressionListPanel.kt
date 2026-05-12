package com.cloveriris.calcore.ui.graphing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.domain.model.graphing.ExpressionType
import com.cloveriris.calcore.domain.model.graphing.GraphExpression
import com.cloveriris.calcore.ui.theme.CalcoreTheme

@Composable
fun ExpressionListPanel(
    expressions: List<GraphExpression>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Expressions",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider()

        if (expressions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add an expression to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = expressions,
                    key = { it.id }
                ) { expression ->
                    ExpressionRow(
                        expression = expression,
                        isSelected = expression.id == selectedId,
                        onSelect = {
                            onSelect(if (selectedId == expression.id) null else expression.id)
                        },
                        onToggleVisibility = { onToggleVisibility(expression.id) },
                        onRemove = { onRemove(expression.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressionRow(
    expression: GraphExpression,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(expression.color)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Type label
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = when (expression.type) {
                    ExpressionType.EXPLICIT_Y -> "y="
                    ExpressionType.POLAR -> "r="
                    ExpressionType.PARAMETRIC -> "param"
                    else -> expression.type.name.lowercase()
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Expression text
        Text(
            text = expression.displayExpression,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Visibility toggle
        IconButton(
            onClick = onToggleVisibility,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (expression.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (expression.isVisible) "Hide" else "Show",
                tint = if (expression.isVisible) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpressionListPanelPreview() {
    CalcoreTheme {
        ExpressionListPanel(
            expressions = listOf(
                GraphExpression(
                    rawExpression = "x^2",
                    color = Color(0xFFFF5252)
                ),
                GraphExpression(
                    rawExpression = "sin(x)",
                    color = Color(0xFF448AFF)
                )
            ),
            selectedId = null,
            onSelect = {},
            onToggleVisibility = {},
            onRemove = {}
        )
    }
}
