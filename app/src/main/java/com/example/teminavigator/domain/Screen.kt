package com.example.teminavigator.domain

/**
 * All screens the UI can show.
 *
 * The ViewModel holds exactly one [Screen] as its current state, and the UI
 * switches between screens by observing it. Being sealed, a `when` over
 * [Screen] must handle every case.
 */
sealed interface Screen {

    /** Start screen where the user picks a destination (by touch or voice). */
    data object Home : Screen

    /**
     * Screen shown while a destination is being guided to, with the map and route.
     * @property destination The destination the user is being guided to.
     */
    data class Navigation(val destination: Destination) : Screen

    /** Screen shown while the robot is idle at its waiting position and no one is using it. */
    data object Waiting : Screen

    /** Password-protected configuration screen for the app options. */
    data object Settings : Screen
}