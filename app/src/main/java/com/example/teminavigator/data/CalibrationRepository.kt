package com.example.teminavigator.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.teminavigator.domain.MapCalibration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferences DataStore holding the persisted map calibration.
 *
 * Declared once at file level, as DataStore requires exactly one instance per
 * file name. Stored under the name "settings".
 */
private val Context.dataStore by preferencesDataStore(name="settings")

/**
 * Persists the [MapCalibration] locally and exposes it as a reactive stream.
 *
 * The calibration ties the map image to the real world. It defines where the
 * robot's origin lies on the image, how many pixels equal one meter, and how
 * the robot's heading is rotated relative to the map. If nothing has been saved
 * yet, built-in default values are used, so [calibration] always emits a
 * usable value.
 *
 * @property context Android context used to access the DataStore.
 */
class CalibrationRepository(private val context: Context){
    /** Preference keys under which the individual calibration values are stored. */
    private object Keys {
        val ORIGIN_X=floatPreferencesKey("origin_x")
        val ORIGIN_Y=floatPreferencesKey("origin_y")
        val PX_PER_METER=floatPreferencesKey("px_per_meter")
        val ANCHOR_YAW=floatPreferencesKey("anchor_yaw")
    }

    /**
     * Live stream of the current map calibration.
     *
     * Emits on collection and again whenever [save] changes a value. Any value
     * that is missing from storage is replaced by its default:
     * - origin: (418, 1191) px – measured home base coordinates in C12 2.72
     * - scale: 24 px per meter – measured scale of initial map image
     * - anchor yaw: -1.6 (radians) – empirically measured yaw
     *   difference between robots in 2.72 and initial map image
     */
    val calibration: Flow<MapCalibration> = context.dataStore.data.map {p->
        MapCalibration(
            originPx = Offset(p[Keys.ORIGIN_X] ?: 418f, p[Keys.ORIGIN_Y] ?: 1191f),
            pxPerMeter = p[Keys.PX_PER_METER] ?: 24f,
            anchorYaw = p[Keys.ANCHOR_YAW] ?: -1.6f
        )
    }

    /**
     * Writes all calibration values to local storage in one transaction.
     *
     * Collectors of [calibration] are notified automatically. Runs asynchronously
     * and does not block the caller's thread.
     *
     * @param c The calibration to persist; overwrites any previously saved values.
     */
    suspend fun save(c: MapCalibration) {
        context.dataStore.edit { p ->
            p[Keys.ORIGIN_X] = c.originPx.x
            p[Keys.ORIGIN_Y] = c.originPx.y
            p[Keys.PX_PER_METER] = c.pxPerMeter
            p[Keys.ANCHOR_YAW] = c.anchorYaw
        }
    }
}