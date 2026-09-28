package com.example.teminavigator.ui.langs

// a data class describing all types of strings in the app
data class AppStrings (
    // Home Screen
    val acceptButton: String,
    val confirmTitle: (destination: String) -> String,
    val confirmYes: String,
    val confirmNo: String,

    // Content descriptions
    val cdMap: String,
    val cdSettings: String,
    val cdZoomIn: String,
    val cdZoomOut: String,

    // Settings
    val settingsTitle: String,
    val languageLabel: String,

    // Spoken by the robot
    val spokenArrived: (destination: String) -> String,
    val spokenUnknownDestination: String,
    )