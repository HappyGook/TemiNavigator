package com.example.teminavigator.data

import kotlinx.serialization.Serializable

/**
 * User-defined labels for a single destination, in the serializable format
 * used for local storage.
 *
 * Stored as part of a map keyed by the destination's temi SDK id
 * (see [DestinationLabelRepository]).
 *
 * @property displayName Name shown to the user in the UI and spoken by the robot.
 * @property aliases Alternative names the destination can be called by, e.g.
 * for voice input. Empty by default.
 */
@Serializable
data class DestinationLabels(
    val displayName: String,
    val aliases: List<String> = emptyList()
)