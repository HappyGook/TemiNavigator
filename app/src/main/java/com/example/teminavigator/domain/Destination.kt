package com.example.teminavigator.domain

import androidx.compose.ui.geometry.Offset

/*
* A place the robot can guide user to
* Additional params to the robot.locations, which returns only names
 */
data class Destination(
    val id: String, // must match the temi internal location name
    val displayName: String,
    val aliases: List<String>,
    val imagePx: Offset? = null,       // already transformed, null = no pose found
    val imageYawDeg: Float? = null,
) {
    companion object {
        fun placeholder(id: String) = Destination(
            id = id,
            displayName = id,
            aliases = listOf(id),
        )
    }
}

fun List<String>.toDestinations(): List<Destination> = map { Destination.placeholder(it) }