package com.example.teminavigator.domain

sealed interface Screen {
    data object Home : Screen
    data class Navigation(val destination: Destination) : Screen
    data object Waiting : Screen
    data object Settings : Screen
}