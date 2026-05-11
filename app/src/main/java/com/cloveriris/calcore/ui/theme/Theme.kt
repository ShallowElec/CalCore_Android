package com.cloveriris.calcore.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Calcore 三套配色方案枚举
 *
 * - [SYSTEM_DYNAMIC]: 跟随 Android 系统 Material You 壁纸取色（API 31+）
 * - [CALCORE_DARK]: 终端绿方案，#0a0a0a 黑底 + #00ff41 科技绿，硬核美学
 * - [CALCORE_LIGHT]: 浅蓝风方案，纯白底 + 浅蓝/蓝色系，用户个人风格
 */
enum class AppTheme {
    SYSTEM_DYNAMIC,
    CALCORE_DARK,
    CALCORE_LIGHT
}

@Composable
fun CalcoreTheme(
    appTheme: AppTheme = AppTheme.SYSTEM_DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.SYSTEM_DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                // API < 31 回退到 Calcore 深色方案
                CalcoreDarkScheme
            }
        }
        AppTheme.CALCORE_DARK -> CalcoreDarkScheme
        AppTheme.CALCORE_LIGHT -> CalcoreLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
