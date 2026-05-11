package com.cloveriris.calcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SquareFoot
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.domain.model.CalculatorMode

/**
 * 汉堡菜单抽屉内容
 *
 * 窄版设计（260dp），带 Acrylic 半透明背景效果。
 */
@Composable
fun DrawerMenu(
    currentMode: CalculatorMode,
    onModeSelected: (CalculatorMode) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Acrylic 背景：半透明深色 + 右侧边缘光
    val acrylicBackground = Color(0xE61C1C1C)
    val edgeGlow = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.04f)
        ),
        startX = 0f,
        endX = 80f
    )

    ModalDrawerSheet(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight(),
        drawerContainerColor = acrylicBackground,
        drawerTonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            // 右侧边缘光（模拟 Acrylic 的光照边缘）
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(edgeGlow)
                    .align(Alignment.CenterEnd)
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
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

                // === 计算器 ===
                DrawerSectionTitle("计算器")
                DrawerItem(
                    label = "标准",
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.STANDARD,
                    onClick = { onModeSelected(CalculatorMode.STANDARD) }
                )
                DrawerItem(
                    label = "科学",
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.SCIENTIFIC,
                    onClick = { onModeSelected(CalculatorMode.SCIENTIFIC) }
                )
                DrawerItem(
                    label = "程序员",
                    icon = { Icon(Icons.Default.SquareFoot, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.PROGRAMMER,
                    onClick = { onModeSelected(CalculatorMode.PROGRAMMER) }
                )
                DrawerItem(
                    label = "日期计算",
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = currentMode == CalculatorMode.DATE,
                    onClick = { onModeSelected(CalculatorMode.DATE) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // === 转换器 ===
                DrawerSectionTitle("转换器")
                val converters = listOf(
                    "货币" to "\u0024",
                    "体积" to "V",
                    "长度" to "L",
                    "重量" to "W",
                    "温度" to "T",
                    "能量" to "E",
                    "面积" to "A",
                    "速度" to "S",
                    "时间" to "t",
                    "功率" to "P",
                    "数据" to "D"
                )
                converters.forEach { (label, _) ->
                    DrawerItem(
                        label = label,
                        icon = {
                            Text(
                                text = label.first().toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
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
