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

/**
 * All simple, user-configurable app settings with their default values.
 *
 * Immutable snapshot emitted by [SettingsRepository.settings]. To change a
 * value, use the matching setter in [SettingsRepository]. To add a setting,
 * add a field here, a key in the repository and a setter.
 *
 * @property returnHomeDelaySec Seconds of inactivity before the robot drives back to its home position.
 * @property arrivalFailureDelaySec Seconds to wait at arrival or after a failure to switch the screen.
 * @property soundsEnabled Whether sound effects are played.
 * @property musicEnabled Whether background music is played.
 * @property voiceOutputEnabled Whether the robot speaks announcements.
 * @property voiceInputEnabled Whether destinations can be chosen by voice.
 * @property autoReturnEnabled Whether the robot returns home automatically when idle.
 * @property homeLocationId Name/id of the temi location used as the waiting position.
 * @property homeRadius Radius around the home position that counts as "at home".
 * @property greetingsEnabled Whether the robot proactively addresses passers-by.
 * @property greetingsCoolDown Minimum time between two greetings, in milliseconds.
 * @property mapImagePath Path to a custom map image, or null to use the bundled map.
 * @property navigationSpeed Speed used when guiding a user.
 */
data class AppSettings(
    val returnHomeDelaySec: Int = 10,
    val arrivalFailureDelaySec: Int = 3,
    val soundsEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val voiceOutputEnabled: Boolean = true,
    val voiceInputEnabled: Boolean = true,
    val autoReturnEnabled: Boolean = true,
    val homeLocationId: String = "home base",
    val homeRadius: Float = 2f, // radius in meters (later multiplied by pxPerMeter)
    val greetingsEnabled: Boolean = true,
    val greetingsCoolDown: Int = 10*1000, // in ms
    val mapImagePath: String? = null,
    val navigationSpeed: NavSpeed = NavSpeed.MEDIUM
)

/**
 * Preferences DataStore holding the app settings.
 *
 * Declared once at file level, as DataStore requires exactly one instance per
 * file name. Stored under the name "app_settings".
 */
private val Context.settingsStore by preferencesDataStore("app_settings")

/**
 * Persists the [AppSettings] locally and exposes them as a reactive stream.
 *
 * The class follows one pattern throughout:
 * - **Keys:** every setting has a typed preference key in [Keys].
 * - **Read:** [settings] maps the raw stored preferences to an [AppSettings]
 *   object. Any missing or invalid value falls back to its default.
 * - **Write:** one `set...` function per setting writes a single value, so
 *   unrelated settings are never overwritten. Numeric setters clamp the value
 *   to a safe range.
 *
 * @property context Android context used to access the DataStore.
 */
class SettingsRepository(private val context: Context) {

    /**
     * Preference keys of all settings.
     *
     * The key strings are the names used on disk. Changing one resets that setting
     * to its default for existing installations.
     */
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

    /**
     * Live stream of the current settings.
     *
     * Emits on collection and again after every change. Each field falls back to
     * the default from [AppSettings] when it has not been stored yet. A stored
     * [NavSpeed] name that can no longer be parsed also falls back to the default.
     */
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

    /*
    * ---- Setters --------------------------------------------------------------
    * All setters below work the same way: they are suspend functions that update
    * exactly one preference in a DataStore transaction, which triggers a new
    * emission of [settings]. Setters for numeric values clamp the input to the
    * allowed range instead of rejecting it:
    *   returnHomeDelay / arrivalFailureDelay: 1..120 s
    *   homeRadius: 0.5..10
    *   greetingsCooldown: 1,000..300,000 ms
    * Boolean setters (sounds, music, voice in/out, auto-return, greetings) store
    * the value as given.
    */

    /**
     * Sets the idle time before the robot returns to its home position.
     *
     * @param sec Delay in seconds; values outside 1..120 are clamped to that range.
     */
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

    /**
     * Sets which temi location is used as the home position.
     *
     * @param name Location name as known to the temi SDK; whitespace is trimmed.
     */
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

    /**
     * Sets or clears the custom map image.
     *
     * @param path Path to the image file, or null to remove the setting and use
     * the default map again.
     */
    suspend fun setMapImagePath(path: String?) {
        context.settingsStore.edit { p ->
            if (path == null) p.remove(Keys.MAP_IMAGE_PATH) else p[Keys.MAP_IMAGE_PATH] = path
        }
    }

    /**
     * Sets the speed used for guiding users.
     *
     * Stored as the enum constant's name, which is why [settings] can recover
     * gracefully if that name no longer exists.
     *
     * @param speed The new navigation speed.
     */
    suspend fun setNavigationSpeed(speed: NavSpeed) {
        context.settingsStore.edit { it[Keys.NAVIGATION_SPEED] = speed.name }
    }
}