package com.example.teminavigator.ui.langs

// a data class describing all types of strings in the app
data class AppStrings (
    // Home Screen
    val acceptButton: String,
    val confirmTitle: (destination: String) -> String,
    val confirmYes: String,
    val confirmNo: String,

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
    val destinationHeader: String,
    val destinationDisplayName: String,
    val destinationAliases: String,
    val destinationButtonSave: String,
    val destinationButtonReset: String,




    // Spoken by the robot
    val spokenArrived: (destination: String) -> String,
    val spokenStarting: (destination: String) -> String,
    val spokenAbort: String,
    val spokenUnknownDestination: String,
    )