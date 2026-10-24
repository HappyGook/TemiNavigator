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


/**
 * Preferences DataStore holding the destination labels.
 *
 * Declared once at file level, as DataStore requires exactly one instance per
 * file name. Stored under the name "destination_labels".
 */
private val Context.destinationDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "destination_labels")


/**
 * Persists custom display names and aliases for destinations locally.
 *
 * The destinations themselves come from the temi SDK, which identifies each by
 * an internal string id (its location name). This repository attaches
 * user-friendly labels ([DestinationLabels]) to those ids without modifying the
 * SDK data. All labels are stored as one JSON string under a single
 * preference key, so every write replaces that string as a whole.
 *
 * @param context Any Android context; the application context is used internally.
 */
class DestinationLabelRepository(context: Context){
    private val store = context.applicationContext.destinationDataStore

    /** Preference key under which the whole label map is stored as a JSON string. */
    private val key = stringPreferencesKey("destination_labels")

    /**
     * Live stream of all stored labels, keyed by temi destination id.
     *
     * Emits an empty map if nothing has been saved yet. Destinations without an
     * entry have no custom labels, so callers should fall back to the raw temi id.
     *
     * @throws kotlinx.serialization.SerializationException if the stored JSON is
     * corrupted and cannot be decoded.
     */
    val labels: Flow<Map<String, DestinationLabels>> = store.data.map { prefs ->
        prefs[key]?.let { Json.decodeFromString<Map<String, DestinationLabels>>(it) }
            ?: emptyMap()
    }

    /**
     * Sets the display name of a destination, keeping its aliases unchanged.
     *
     * If the destination has no entry yet, one is created.
     *
     * @param id Internal temi id of the destination.
     * @param name New display name; leading and trailing whitespace is removed.
     */
    suspend fun setDisplayName(id: String, name: String) = update(id) {
        it.copy(displayName = name.trim())
    }

    /**
     * Replaces the aliases of a destination, keeping its display name unchanged.
     *
     * The list is cleaned before saving: entries are trimmed, blank entries are
     * dropped and duplicates are removed.
     *
     * @param id Internal temi id of the destination.
     * @param aliases New list of alternative names.
     */
    suspend fun setAliases(id: String, aliases: List<String>) = update(id) {
        it.copy(aliases = aliases.map(String::trim).filter(String::isNotEmpty).distinct())
    }

    /**
     * Removes all custom labels of a destination.
     *
     * Afterwards the destination has no entry in [labels] and falls back to its
     * temi id. Does nothing if no entry exists.
     *
     * @param id Internal temi id of the destination.
     */
    suspend fun reset(id: String) = store.edit { prefs ->
        val current = prefs[key]?.let { Json.decodeFromString<Map<String, DestinationLabels>>(it) } ?: emptyMap()
        prefs[key] = Json.encodeToString(current - id)
    }

    /**
     * Read-modify-write helper shared by the setters.
     *
     * Loads the stored map, applies [transform] to the entry for [id] and writes
     * the map back inside a single DataStore transaction. A missing entry starts
     * from `DestinationLabels(displayName = id)`.
     *
     * @param id Internal temi id of the destination to change.
     * @param transform Function producing the new labels from the old ones.
     */
    private suspend fun update(id: String, transform: (DestinationLabels) -> DestinationLabels) {
        store.edit { prefs ->
            val current = prefs[key]?.let { Json.decodeFromString<Map<String, DestinationLabels>>(it) } ?: emptyMap()
            val old = current[id] ?: DestinationLabels(displayName = id)
            prefs[key] = Json.encodeToString(current + (id to transform(old)))
        }
    }
}