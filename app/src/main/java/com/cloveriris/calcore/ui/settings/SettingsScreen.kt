package com.cloveriris.calcore.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.ui.theme.AppTheme
import com.cloveriris.calcore.ui.theme.CalcoreTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme = AppTheme.SYSTEM_DYNAMIC,
    onThemeChange: (AppTheme) -> Unit = {},
    currentArchitecture: Architecture = Architecture.X86_64,
    onArchitectureChange: (Architecture) -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ===== Theme Section =====
            SectionHeader("Appearance")
            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ===== Architecture Section =====
            SectionHeader("Simulation Architecture")
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select the processor architecture for visualization. " +
                        "This affects register names, stack pointer labels, and instruction mnemonics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Architecture.entries.forEach { arch ->
                ThemeOption(
                    label = arch.displayName,
                    selected = currentArchitecture == arch,
                    onClick = { onArchitectureChange(arch) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ===== Animation Speed Section =====
            SectionHeader("Animation Speed")
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Adjust visualization playback speed. " +
                        "Lower values slow down each step for better observation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "0.2x",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = playbackSpeed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.2f..2.0f,
                    steps = 8, // 0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text(
                    text = "2.0x",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Current: ${"%.1f".format(playbackSpeed)}x  (~${(200f / playbackSpeed).toInt()}ms per step)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
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
            .padding(vertical = 2.dp),
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
        SettingsScreen(
            currentArchitecture = Architecture.ARM64,
            playbackSpeed = 0.8f
        )
    }
}
