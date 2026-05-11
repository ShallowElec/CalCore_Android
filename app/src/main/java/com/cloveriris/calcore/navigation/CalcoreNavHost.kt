package com.cloveriris.calcore.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.cloveriris.calcore.ui.calculator.CalculatorScreen
import com.cloveriris.calcore.ui.graphing.GraphingScreen
import com.cloveriris.calcore.ui.settings.SettingsScreen
import com.cloveriris.calcore.ui.theme.AppTheme

object CalcoreDestinations {
    const val CALCULATOR = "calculator"
    const val GRAPHING = "graphing"
    const val SETTINGS = "settings"
}

@Composable
fun CalcoreNavHost(
    navController: NavHostController,
    currentTheme: AppTheme = AppTheme.SYSTEM_DYNAMIC,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onThemeChange: (AppTheme) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = CalcoreDestinations.CALCULATOR,
        modifier = modifier
    ) {
        composable(CalcoreDestinations.CALCULATOR) {
            CalculatorScreen(
                windowWidthSizeClass = windowWidthSizeClass,
                onNavigateToGraphing = {
                    navController.navigate(CalcoreDestinations.GRAPHING)
                },
                onNavigateToSettings = {
                    navController.navigate(CalcoreDestinations.SETTINGS)
                }
            )
        }
        composable(CalcoreDestinations.GRAPHING) {
            GraphingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(CalcoreDestinations.SETTINGS) {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
