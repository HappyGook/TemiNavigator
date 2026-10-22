package com.example.teminavigator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.teminavigator.domain.NavSpeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Put all simple settings values here
data class AppSettings(
    val returnHomeDelaySec: Int = 10,
    val arrivalFailureDelaySec: Int = 3,
    val soundsEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val voiceOutputEnabled: Boolean = true,
    val voiceInputEnabled: Boolean = true,
    val autoReturnEnabled: Boolean = true,
    val homeLocationId: String = "home base",
    val homeRadius: Float = 2f, // radius in pixels (later multiplied by pxPerMeter)
    val greetingsEnabled: Boolean = true,
    val greetingsCoolDown: Int = 10*1000, // in ms
    val mapImagePath: String? = null,
    val navigationSpeed: NavSpeed = NavSpeed.MEDIUM
)

private val Context.settingsStore by preferencesDataStore("app_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val RETURN_HOME_DELAY = intPreferencesKey("return_home_delay_sec")
        val ARRIVAL_FAILURE_DELAY = intPreferencesKey("arrival_failure_delay_sec")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val VOICE_OUTPUT_ENABLED = booleanPreferencesKey("voice_output_enabled")
        val VOICE_INPUT_ENABLED = booleanPreferencesKey("voice_input_enabled")
        val AUTO_RETURN_ENABLED = booleanPreferencesKey("auto_return_enabled")
        val HOME_LOCATION_NAME = stringPreferencesKey("home_location_name")
        val HOME_RADIUS = floatPreferencesKey("home_radius")
        val GREETINGS_ENABLED = booleanPreferencesKey("greetings_enabled")
        val GREETINGS_COOLDOWN = intPreferencesKey("greetings_cooldown")
        val MAP_IMAGE_PATH = stringPreferencesKey("map_image_path")
        val NAVIGATION_SPEED = stringPreferencesKey("navigation_speed")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        val d = AppSettings() // default values
        AppSettings(
            returnHomeDelaySec = p[Keys.RETURN_HOME_DELAY] ?: d.returnHomeDelaySec,
            arrivalFailureDelaySec = p[Keys.ARRIVAL_FAILURE_DELAY] ?: d.arrivalFailureDelaySec,
            soundsEnabled = p[Keys.SOUNDS_ENABLED] ?: d.soundsEnabled,
            musicEnabled = p[Keys.MUSIC_ENABLED] ?: d.musicEnabled,
            voiceOutputEnabled = p[Keys.VOICE_OUTPUT_ENABLED] ?: d.voiceOutputEnabled,
            voiceInputEnabled = p[Keys.VOICE_INPUT_ENABLED] ?: d.voiceInputEnabled,
            autoReturnEnabled = p[Keys.AUTO_RETURN_ENABLED] ?: d.autoReturnEnabled,

            homeLocationId = p[Keys.HOME_LOCATION_NAME] ?: d.homeLocationId,
            homeRadius = p[Keys.HOME_RADIUS] ?: d.homeRadius,

            greetingsEnabled = p[Keys.GREETINGS_ENABLED] ?: d.greetingsEnabled,
            greetingsCoolDown = p[Keys.GREETINGS_COOLDOWN] ?: d.greetingsCoolDown,

            mapImagePath = p[Keys.MAP_IMAGE_PATH] ?: d.mapImagePath,
            navigationSpeed = p[Keys.NAVIGATION_SPEED]
                ?.let { runCatching { NavSpeed.valueOf(it) }.getOrNull() }
                ?: d.navigationSpeed
        )
    }

    suspend fun setReturnHomeDelay(sec: Int) {
        context.settingsStore.edit { it[Keys.RETURN_HOME_DELAY] = sec.coerceIn(1, 120) }
    }
    suspend fun setArrivalFailureDelay(sec: Int) {
        context.settingsStore.edit { it[Keys.ARRIVAL_FAILURE_DELAY] = sec.coerceIn(1, 120) }
    }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.SOUNDS_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
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

    suspend fun setHomeLocationName(name: String) {
        context.settingsStore.edit { it[Keys.HOME_LOCATION_NAME] = name.trim() }
    }

    suspend fun setHomeRadius(pixels: Float) {
        context.settingsStore.edit { it[Keys.HOME_RADIUS] = pixels.coerceIn(0.5f, 10f) }
    }

    suspend fun setGreetingsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.GREETINGS_ENABLED] = enabled }
    }

    suspend fun setGreetingsCooldown(ms: Int) {
        context.settingsStore.edit { it[Keys.GREETINGS_COOLDOWN] = ms.coerceIn(1_000, 300_000) }
    }

    suspend fun setMapImagePath(path: String?) {
        context.settingsStore.edit { p ->
            if (path == null) p.remove(Keys.MAP_IMAGE_PATH) else p[Keys.MAP_IMAGE_PATH] = path
        }
    }

    suspend fun setNavigationSpeed(speed: NavSpeed) {
        context.settingsStore.edit { it[Keys.NAVIGATION_SPEED] = speed.name }
    }
}