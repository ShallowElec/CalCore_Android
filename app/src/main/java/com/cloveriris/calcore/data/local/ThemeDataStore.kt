package com.cloveriris.calcore.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cloveriris.calcore.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemeDataStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme")

    val appTheme: Flow<AppTheme> = context.themeDataStore.data
        .map { preferences ->
            val themeName = preferences[themeKey] ?: AppTheme.SYSTEM_DYNAMIC.name
            AppTheme.entries.find { it.name == themeName } ?: AppTheme.SYSTEM_DYNAMIC
        }

    suspend fun setAppTheme(theme: AppTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }
}
