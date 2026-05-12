package com.cloveriris.calcore.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cloveriris.calcore.domain.model.Architecture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_preferences")

/**
 * 通用设置 DataStore
 *
 * 持久化 Architecture、PlaybackSpeed 等用户偏好。
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val architectureKey = stringPreferencesKey("architecture")
    private val playbackSpeedKey = floatPreferencesKey("playback_speed")

    val architecture: Flow<Architecture> = context.settingsDataStore.data
        .map { preferences ->
            val name = preferences[architectureKey] ?: Architecture.X86_64.name
            Architecture.entries.find { it.name == name } ?: Architecture.X86_64
        }

    val playbackSpeed: Flow<Float> = context.settingsDataStore.data
        .map { preferences ->
            (preferences[playbackSpeedKey] ?: 1.0f).coerceIn(0.2f, 2.0f)
        }

    suspend fun setArchitecture(arch: Architecture) {
        context.settingsDataStore.edit { preferences ->
            preferences[architectureKey] = arch.name
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[playbackSpeedKey] = speed.coerceIn(0.2f, 2.0f)
        }
    }
}
