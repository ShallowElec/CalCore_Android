package com.cloveriris.calcore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.domain.model.template.MathTemplate
import com.cloveriris.calcore.domain.model.template.TemplateCategory
import com.cloveriris.calcore.domain.model.template.TemplateLibrary

@Composable
fun TemplateSelector(
    templates: List<MathTemplate>,
    onTemplateSelected: (MathTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = templates.map { it.category }.distinct().sortedBy { it.ordinal }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        if (categories.size > 1) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        val filteredTemplates = if (categories.isEmpty()) {
            emptyList()
        } else {
            templates.filter { it.category == categories.getOrNull(selectedCategoryIndex) }
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredTemplates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: MathTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (template.parameters.isNotEmpty()) {
                Text(
                    text = template.parameters.joinToString(", ") { "${it.symbol}=${it.defaultValue}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val TemplateCategory.displayName: String
    get() = when (this) {
        TemplateCategory.GRAPHING -> "图形"
        TemplateCategory.LINEAR_ALGEBRA -> "线性代数"
        TemplateCategory.CALCULUS -> "微积分"
        TemplateCategory.ORDINARY_DIFF_EQ -> "常微分方程"
        TemplateCategory.PARTIAL_DIFF_EQ -> "偏微分方程"
    }

@Preview(showBackground = true)
@Composable
private fun TemplateSelectorPreview() {
    com.cloveriris.calcore.ui.theme.CalcoreTheme {
        TemplateSelector(
            templates = TemplateLibrary.all,
            onTemplateSelected = {}
        )
    }
}
