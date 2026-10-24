package com.example.teminavigator.domain

import androidx.compose.ui.geometry.Offset

/**
 * A place the robot can guide the user to.
 *
 * Extends the plain location names of the Temi SDK (`robot.locations`) with
 * user-friendly labels and the position on the map image. The labels come from
 * [com.example.teminavigator.data.DestinationLabelRepository] and the pixel position is calculated with
 * [MapCalibration].
 *
 * @property id Internal Temi location name. Must match the SDK exactly, since
 * it is passed to [RobotController.goTo].
 * @property displayName Name shown to the user and spoken by the robot.
 * @property aliases Alternative names that are also accepted, e.g. for speech
 * input (see [match]).
 * @property imagePx Position on the map image in pixels (already transformed),
 * or null if no pose was found for this location.
 * @property imageYawDeg Heading at the destination in degrees, or null if
 * unknown.
 */
data class Destination(
    val id: String, // must match the temi internal location name
    val displayName: String,
    val aliases: List<String>,
    val imagePx: Offset? = null,       // already transformed, null = no pose found
    val imageYawDeg: Float? = null,
)