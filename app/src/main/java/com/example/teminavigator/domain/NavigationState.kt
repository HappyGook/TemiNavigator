package com.example.teminavigator.domain

/**
 * All states the navigation process can be in at any moment.
 *
 * It is the ViewModel's single source of truth for what the robot is doing
 * with regard to navigation. A sealed interface works like an enum whose
 * entries can carry different data, and a `when` over it must handle every
 * state. Sealed also means no new states can be added outside this module.
 */
sealed interface NavigationState{

    /** No guidance is running. The robot is waiting for a destination. */
    data object Idle: NavigationState

    /**
     * The robot is currently guiding the user to a destination.
     *
     * @property target The destination being navigated to.
     */
    data class Guiding(val target: Destination) : NavigationState

    /**
     * Guidance was paused by the user and can be resumed or cancelled.
     *
     * @property target The destination of the paused guidance.
     */
    data class Paused(val target: Destination) : NavigationState

    /**
     * The robot reached its destination and the arrival is shown to the user.
     *
     * @property target The destination that was reached.
     */
    data class Arrived(val target: Destination) : NavigationState
}