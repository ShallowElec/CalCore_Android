package com.cloveriris.calcore.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.cloveriris.calcore.presentation.visualization.VisualizationViewModel
import com.cloveriris.calcore.ui.calculator.CalculatorScreen
import com.cloveriris.calcore.ui.calculus.CalculusScreen
import com.cloveriris.calcore.ui.diffeq.DifferentialEquationsScreen
import com.cloveriris.calcore.ui.graphing.GraphingScreen
import com.cloveriris.calcore.ui.linearalgebra.LinearAlgebraScreen
import com.cloveriris.calcore.ui.about.AboutScreen
import com.cloveriris.calcore.ui.settings.SettingsScreen
import com.cloveriris.calcore.ui.theme.AppTheme

object CalcoreDestinations {
    const val CALCULATOR = "calculator"
    const val GRAPHING = "graphing"
    const val LINEAR_ALGEBRA = "linear_algebra"
    const val CALCULUS = "calculus"
    const val DIFFERENTIAL_EQUATIONS = "differential_equations"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
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
                onNavigateToLinearAlgebra = {
                    navController.navigate(CalcoreDestinations.LINEAR_ALGEBRA)
                },
                onNavigateToCalculus = {
                    navController.navigate(CalcoreDestinations.CALCULUS)
                },
                onNavigateToDifferentialEquations = {
                    navController.navigate(CalcoreDestinations.DIFFERENTIAL_EQUATIONS)
                },
                onNavigateToSettings = {
                    navController.navigate(CalcoreDestinations.SETTINGS)
                },
                onNavigateToAbout = {
                    navController.navigate(CalcoreDestinations.ABOUT)
                }
            )
        }
        composable(CalcoreDestinations.GRAPHING) {
            GraphingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(CalcoreDestinations.LINEAR_ALGEBRA) {
            LinearAlgebraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(CalcoreDestinations.CALCULUS) {
            CalculusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(CalcoreDestinations.DIFFERENTIAL_EQUATIONS) {
            DifferentialEquationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(CalcoreDestinations.ABOUT) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(CalcoreDestinations.SETTINGS) { backStackEntry ->
            // 共享 Calculator 目的地的 VisualizationViewModel，确保设置与计算器使用同一实例
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(CalcoreDestinations.CALCULATOR)
            }
            val visualizationViewModel: VisualizationViewModel = hiltViewModel(parentEntry)
            val visState by visualizationViewModel.uiState.collectAsStateWithLifecycle()

            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                currentArchitecture = visState.architecture,
                onArchitectureChange = visualizationViewModel::setArchitecture,
                playbackSpeed = visState.playbackSpeed,
                onSpeedChange = visualizationViewModel::setPlaybackSpeed,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
