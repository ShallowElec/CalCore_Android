package com.cloveriris.calcore.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.ui.theme.AppTheme
import com.cloveriris.calcore.ui.theme.CalcoreTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme = AppTheme.SYSTEM_DYNAMIC,
    onThemeChange: (AppTheme) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ThemeOption(
                label = "System Dynamic",
                selected = currentTheme == AppTheme.SYSTEM_DYNAMIC,
                onClick = { onThemeChange(AppTheme.SYSTEM_DYNAMIC) }
            )
            ThemeOption(
                label = "Calcore Dark (Terminal Green)",
                selected = currentTheme == AppTheme.CALCORE_DARK,
                onClick = { onThemeChange(AppTheme.CALCORE_DARK) }
            )
            ThemeOption(
                label = "Calcore Light (Sky Blue)",
                selected = currentTheme == AppTheme.CALCORE_LIGHT,
                onClick = { onThemeChange(AppTheme.CALCORE_LIGHT) }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = 0.dp
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    CalcoreTheme {
        SettingsScreen()
    }
}
