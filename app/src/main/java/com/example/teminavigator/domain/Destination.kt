package com.example.teminavigator.domain

/*
* A place the robot can guide user to
* Additional params to the robot.locations, which returns only names
 */
data class Destination(
    val id: String, // must match the temi internal location name
    val displayName: String,
    val aliases: List<String>, // for speech: 2.72 = "Raum 2.72", "Robotik-Raum", etc.
    val mapX: Float,
    val mapY: Float // coordinates on internal map
)