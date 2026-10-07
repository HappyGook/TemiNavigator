package com.example.teminavigator.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.teminavigator.domain.MapCalibration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name="settings")

class CalibrationRepository(private val context: Context){
    private object Keys {
        val ORIGIN_X=floatPreferencesKey("origin_x")
        val ORIGIN_Y=floatPreferencesKey("origin_y")
        val PX_PER_METER=floatPreferencesKey("px_per_meter")
        val ANCHOR_YAW=floatPreferencesKey("anchor_yaw")
    }

    val calibration: Flow<MapCalibration> = context.dataStore.data.map {p->
        MapCalibration(
            originPx = Offset(p[Keys.ORIGIN_X] ?: 418f, p[Keys.ORIGIN_Y] ?: 1191f),
            pxPerMeter = p[Keys.PX_PER_METER] ?: 24f,
            anchorYaw = p[Keys.ANCHOR_YAW] ?: -1.6f
        )
    }

    suspend fun save(c: MapCalibration) {
        context.dataStore.edit { p ->
            p[Keys.ORIGIN_X] = c.originPx.x
            p[Keys.ORIGIN_Y] = c.originPx.y
            p[Keys.PX_PER_METER] = c.pxPerMeter
            p[Keys.ANCHOR_YAW] = c.anchorYaw
        }
    }
}