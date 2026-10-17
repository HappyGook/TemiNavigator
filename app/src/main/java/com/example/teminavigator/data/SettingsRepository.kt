package com.example.teminavigator.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Put all simple settings values here
data class AppSettings(
    val returnHomeDelaySec: Int = 3,
)

private val Context.settingsStore by preferencesDataStore("app_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val RETURN_HOME_DELAY = intPreferencesKey("return_home_delay_sec")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(returnHomeDelaySec = p[Keys.RETURN_HOME_DELAY] ?: 3)
    }

    suspend fun setReturnHomeDelay(sec: Int) {
        context.settingsStore.edit { it[Keys.RETURN_HOME_DELAY] = sec.coerceIn(1, 120) }
    }
}