package com.example.teminavigator.ui.langs

// a data class describing all types of strings in the app
data class AppStrings (
    // Home Screen
    val acceptButton: String,
    val confirmTitle: (destination: String) -> String,
    val confirmYes: String,
    val confirmNo: String,
    val searchDestinations: String,
    val speechUnavailable: String,

    // Waiting screen
    val greeting: String,
    val locationQuery: String,
    val selectLocation: String,
    val backToNavigation: String,
    val speechNotice: String,

    // Navigation screen
    val goingTo: String,
    val minutes: String,
    val pauseNavigation: String,
    val resumeNavigation: String,
    val abortNavigation: String,
    val simulateArrival: String,

    // Content descriptions
    val cdMap: String,
    val cdSettings: String,
    val cdZoomIn: String,
    val cdZoomOut: String,

    // Settings
    val settingsTitle: String,
    val adminPasswordTitle: String,
    val adminPasswordPrompt: String,
    val adminPasswordLabel: String,
    val unlockSettingsButton: String,
    val incorrectAdminPassword: String,
    val changeAdminPasswordTitle: String,
    val newAdminPasswordLabel: String,
    val confirmAdminPasswordLabel: String,
    val saveAdminPasswordButton: String,
    val adminPasswordTooShort: String,
    val adminPasswordsDoNotMatch: String,
    val adminPasswordChanged: String,
    val languageLabel: String,
    val sounds: String,
    val descriptionSounds: String,
    val voiceOutput: String,
    val voiceInput: String,
    val descriptionVoiceInput: String,
    val descriptionVoiceOutput: String,
    val autoReturn: String,
    val descriptionAutoReturn: String,
    val calibrationHeader: String,
    val originXText: String,
    val originYText: String,
    val pxPerMeterText: String,
    val pxPerMeterWarning: String,
    val anchorYawText: String,
    val saveCalibrationButton: String,



    // Spoken by the robot
    val spokenArrived: (destination: String) -> String,
    val spokenUnknownDestination: String,
    )