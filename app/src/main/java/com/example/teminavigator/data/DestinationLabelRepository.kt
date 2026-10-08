package com.example.teminavigator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.serialization.json.Json


private val Context.destinationDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "destination_labels")


class DestinationLabelRepository(context: Context){
    private val store = context.applicationContext.destinationDataStore
    private val key = stringPreferencesKey("destination_labels")

    // Map<id, labels>, id - internal temi string name
    val labels: Flow<Map<String, DestinationLabels>> = store.data.map { prefs ->
        prefs[key]?.let { Json.decodeFromString<Map<String, DestinationLabels>>(it) }
            ?: emptyMap()
    }

    // async and unblocking updates
    suspend fun setDisplayName(id: String, name: String) = update(id) {
        it.copy(displayName = name.trim())
    }

    suspend fun setAliases(id: String, aliases: List<String>) = update(id) {
        it.copy(aliases = aliases.map(String::trim).filter(String::isNotEmpty).distinct())
    }

    suspend fun reset(id: String) = store.edit { prefs ->
        val current = prefs[key]?.let { Json.decodeFromString<Map<String, DestinationLabels>>(it) } ?: emptyMap()
        prefs[key] = Json.encodeToString(current - id)
    }

    private suspend fun update(id: String, transform: (DestinationLabels) -> DestinationLabels) {
        store.edit { prefs ->
            val current = prefs[key]?.let { Json.decodeFromString<Map<String, DestinationLabels>>(it) } ?: emptyMap()
            val old = current[id] ?: DestinationLabels(displayName = id)
            prefs[key] = Json.encodeToString(current + (id to transform(old)))
        }
    }
}