package com.cloveriris.calcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.cloveriris.calcore.data.local.ThemeDataStore
import com.cloveriris.calcore.navigation.CalcoreNavHost
import com.cloveriris.calcore.ui.theme.AppTheme
import com.cloveriris.calcore.ui.theme.CalcoreTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeDataStore: ThemeDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by themeDataStore.appTheme.collectAsState(initial = AppTheme.SYSTEM_DYNAMIC)
            val scope = rememberCoroutineScope()

            CalcoreTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalcoreApp(
                        currentTheme = appTheme,
                        onThemeChange = { theme ->
                            scope.launch {
                                themeDataStore.setAppTheme(theme)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CalcoreApp(
    currentTheme: AppTheme = AppTheme.SYSTEM_DYNAMIC,
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val navController = rememberNavController()
    CalcoreNavHost(
        navController = navController,
        currentTheme = currentTheme,
        onThemeChange = onThemeChange
    )
}
