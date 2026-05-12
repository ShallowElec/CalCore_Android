package com.cloveriris.calcore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.CalculatorMode

/**
 * 汉堡菜单抽屉内容
 *
 * 跟随 Material 3 主题默认配色。
 */
@Composable
fun DrawerMenu(
    currentMode: CalculatorMode,
    currentDestination: String = "",
    onModeSelected: (CalculatorMode) -> Unit,
    onNavigateToGraphing: () -> Unit = {},
    onNavigateToLinearAlgebra: () -> Unit = {},
    onNavigateToCalculus: () -> Unit = {},
    onNavigateToDifferentialEquations: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
                Spacer(modifier = Modifier.height(24.dp))

                // 标题
                Text(
                    text = "Calcore",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // === 计算 ===
                DrawerSectionTitle("计算")
                DrawerItem(
                    label = "标准",
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.STANDARD && currentDestination.isEmpty(),
                    onClick = { onModeSelected(CalculatorMode.STANDARD) }
                )
                DrawerItem(
                    label = "科学",
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.SCIENTIFIC && currentDestination.isEmpty(),
                    onClick = { onModeSelected(CalculatorMode.SCIENTIFIC) }
                )
                DrawerItem(
                    label = "程序员",
                    icon = { Icon(Icons.Default.SquareFoot, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.PROGRAMMER && currentDestination.isEmpty(),
                    onClick = { onModeSelected(CalculatorMode.PROGRAMMER) }
                )
                DrawerItem(
                    label = "日期计算",
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.DATE && currentDestination.isEmpty(),
                    onClick = { onModeSelected(CalculatorMode.DATE) }
                )
                DrawerItem(
                    label = "图形",
                    icon = {
                        Text(
                            text = "ƒ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selected = currentDestination == "graphing",
                    onClick = onNavigateToGraphing
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // === 数学工作台 ===
                DrawerSectionTitle("数学工作台")
                DrawerItem(
                    label = "线性代数",
                    icon = {
                        Text(
                            text = "M",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selected = currentDestination == "linear_algebra",
                    onClick = onNavigateToLinearAlgebra
                )
                DrawerItem(
                    label = "微积分",
                    icon = {
                        Text(
                            text = "∫",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selected = currentDestination == "calculus",
                    onClick = onNavigateToCalculus
                )
                DrawerItem(
                    label = "微分方程",
                    icon = {
                        Text(
                            text = "∂",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selected = currentDestination == "differential_equations",
                    onClick = onNavigateToDifferentialEquations
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // === 转换器 ===
                DrawerSectionTitle("转换器")
                val converters = listOf(
                    "货币" to Icons.Default.Payments,
                    "体积" to Icons.Default.Opacity,
                    "长度" to Icons.Default.Straighten,
                    "重量" to Icons.Default.Scale,
                    "温度" to Icons.Default.Thermostat,
                    "能量" to Icons.Default.Bolt,
                    "面积" to Icons.Default.CropFree,
                    "速度" to Icons.Default.Speed,
                    "时间" to Icons.Default.AccessTime,
                    "功率" to Icons.Default.PowerSettingsNew,
                    "数据" to Icons.Default.SdStorage
                )
                converters.forEach { (label, iconVector) ->
                    DrawerItem(
                        label = label,
                        icon = {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        selected = false,
                        onClick = { /* TODO: converter navigation */ }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // === 其他 ===
                DrawerSectionTitle("其他")
                DrawerItem(
                    label = "设置",
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = false,
                    onClick = onNavigateToSettings
                )
                DrawerItem(
                    label = "关于",
                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = false,
                    onClick = onNavigateToAbout
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = icon,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    )
}
