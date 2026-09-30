package com.example.teminavigator.domain

/*
* A place the robot can guide user to
* Additional params to the robot.locations, which returns only names
 */
data class Destination(
    val id: String, // must match the temi internal location name
    val displayName: String,
    val aliases: List<String>,
    val mapX: Float,
    val mapY: Float
) {
    companion object {
        fun placeholder(id: String) = Destination(
            id = id,
            displayName = id,
            aliases = listOf(id),
            mapX = 0f,
            mapY = 0f
        )
    }
}

fun List<String>.toDestinations(): List<Destination> = map { Destination.placeholder(it) }