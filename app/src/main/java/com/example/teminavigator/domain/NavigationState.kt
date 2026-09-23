package com.example.teminavigator.domain

/*
* All states the navigation process can be in a moment
* Sealed interface here is kinda like an enum for navigation states,
* but states can have different params and must be handled in when statements
 */
//
sealed interface NavigationState{ // sealed so can't be implemented after compiling
    data object Idle: NavigationState
    data class Proposing(val target: Destination) : NavigationState
    data class Guiding(val target: Destination) : NavigationState
    data class Paused(val target: Destination) : NavigationState
    data class Arrived(val target: Destination) : NavigationState
    data class Error(val message: String) : NavigationState
}