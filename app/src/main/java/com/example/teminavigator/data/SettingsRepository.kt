package com.example.teminavigator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Put all simple settings values here
data class AppSettings(
    val returnHomeDelaySec: Int = 3,
    val soundsEnabled: Boolean = true,
    val voiceOutputEnabled: Boolean = true,
    val voiceInputEnabled: Boolean = true,
    val autoReturnEnabled: Boolean = true,
)

private val Context.settingsStore by preferencesDataStore("app_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val RETURN_HOME_DELAY = intPreferencesKey("return_home_delay_sec")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val VOICE_OUTPUT_ENABLED = booleanPreferencesKey("voice_output_enabled")
        val VOICE_INPUT_ENABLED = booleanPreferencesKey("voice_input_enabled")
        val AUTO_RETURN_ENABLED = booleanPreferencesKey("auto_return_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            returnHomeDelaySec = p[Keys.RETURN_HOME_DELAY] ?: 3,
            soundsEnabled = p[Keys.SOUNDS_ENABLED] ?: true,
            voiceOutputEnabled = p[Keys.VOICE_OUTPUT_ENABLED] ?: true,
            voiceInputEnabled = p[Keys.VOICE_INPUT_ENABLED] ?: true,
            autoReturnEnabled = p[Keys.AUTO_RETURN_ENABLED] ?: true,
        )
    }

    suspend fun setReturnHomeDelay(sec: Int) {
        context.settingsStore.edit { it[Keys.RETURN_HOME_DELAY] = sec.coerceIn(1, 120) }
    }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.SOUNDS_ENABLED] = enabled }
    }

    suspend fun setVoiceOutputEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.VOICE_OUTPUT_ENABLED] = enabled }
    }

    suspend fun setVoiceInputEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.VOICE_INPUT_ENABLED] = enabled }
    }

    suspend fun setAutoReturnEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.AUTO_RETURN_ENABLED] = enabled }
    }
}