package com.cloveriris.calcore.ui.graphing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.ui.theme.CalcoreTheme

@Composable
fun ParameterSliderPanel(
    parameters: Map<String, Double>,
    onParameterChange: (String, Double) -> Unit,
    onAddParameter: (String, Double, ClosedFloatingPointRange<Double>) -> Unit,
    onRemoveParameter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track custom ranges for parameters; default to -10..10
    var customRanges by remember { mutableStateOf<Map<String, ClosedFloatingPointRange<Double>>>(emptyMap()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Parameters",
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add parameter"
                )
            }
        }

        HorizontalDivider()

        if (parameters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No parameters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                parameters.forEach { (name, value) ->
                    val range = customRanges[name] ?: (-10.0..10.0)
                    // Auto-expand range if value goes outside
                    val adjustedRange = if (value < range.start) {
                        value..range.endInclusive
                    } else if (value > range.endInclusive) {
                        range.start..value
                    } else {
                        range
                    }
                    // Update stored range if expanded
                    if (adjustedRange != range) {
                        customRanges = customRanges + (name to adjustedRange)
                    }

                    ParameterSlider(
                        name = name,
                        value = value,
                        range = adjustedRange,
                        onValueChange = { onParameterChange(name, it) },
                        onRemove = { onRemoveParameter(name) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddParameterDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                val defaultValue = 0.0
                val defaultRange = -10.0..10.0
                customRanges = customRanges + (name to defaultRange)
                onAddParameter(name, defaultValue, defaultRange)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ParameterSlider(
    name: String,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    onValueChange: (Double) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            TextButton(onClick = onRemove) {
                Text("Remove", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            text = String.format("%.3f", value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AddParameterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Parameter") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Parameter name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ParameterSliderPanelPreview() {
    CalcoreTheme {
        ParameterSliderPanel(
            parameters = mapOf("a" to 1.5, "b" to -3.0),
            onParameterChange = { _, _ -> },
            onAddParameter = { _, _, _ -> },
            onRemoveParameter = {}
        )
    }
}
