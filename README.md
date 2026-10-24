# Temi Navigator

## Table of Contents

1. [Overview](#overview)
2. [Settings](#settings)
    - [Admin access](#admin-access)
    - [Navigation settings](#navigation-settings)
    - [Basic settings](#basic-settings)
    - [Language](#language)
3. [Project structure](#project-structure)
4. [AI usage log](#ai-usage-log)


## Overview

This navigator is an Android app that can be used on the [Temi v3](https://www.robotemi.com/product/temi/). 
The application is designed to guide people to destinations stored within the robot.

## Settings

There are several settings to adjust in the settings screen of the application.

### Admin access

The default password to access the settings of the app is by default set to ``admin``.
The default password is changeable in the code via the [AdminPasswordStore](app/src/main/java/com/example/teminavigator/data/AdminPasswordStore.kt) class.
The password is also changeable in the settings menu of the app.

### Navigation settings

There are special settings to correctly adjust the Temi internal map to the used map of the application.
- Map calibration: calibrate the internal map to the displayed map on the screen (requires measurements)
- Choose map: Upload new map to display on the screen
- Destinations: Change display names of destinations

### Basic Settings

- Sound settings: Enable / disable notification sounds
- Voice Output: Enable / disable voice output from the Temi robot
- Voice Input: Enable / disable the possibility for voice input from the user
- Auto Return: Enable / disable auto return to Temi's home base
- Greetings: Enable / disable Temi recognizing nearby people and greeting them
- Navigation Speed: Set speed of the robot to slow / medium / high

### Language

Currently, there are 5 different languages to choose from.
These can be adjusted in the settings menu without requiring an admin password.

## Project structure

```
app/
└── src/main/java/com/example/teminavigator/
    ├── MainActivity.kt
    ├── TemiRobot.kt
    │
    ├── data/
    │   ├── AdminPasswordStore.kt
    │   ├── CalibrationRepository.kt
    │   ├── DestinationLabelRepository.kt
    │   ├── DestinationLabels.kt
    │   └── SettingsRepository.kt
    │
    ├── domain/
    │   ├── Destination.kt
    │   ├── DestinationMatching.kt
    │   ├── MapCalibration.kt
    │   ├── MapLoader.kt
    │   ├── NavSpeed.kt
    │   ├── NavigationState.kt
    │   ├── RobotController.kt
    │   ├── RobotEvent.kt
    │   └── Screen.kt
    │
    ├── ui/
    │   ├── langs/
    │   │   ├── AppLanguage.kt
    │   │   ├── AppStrings.kt
    │   │   ├── EnglishStrings.kt
    │   │   ├── GermanStrings.kt
    │   │   ├── LocalStrings.kt
    │   │   ├── NadsatStrings.kt
    │   │   ├── PirateStrings.kt
    │   │   └── RussianStrings.kt
    │   │
    │   ├── map/
    │   │   ├── InteractiveMap.kt
    │   │   └── MapHelper.kt
    │   │
    │   ├── screens/
    │   │   ├── HomeScreen.kt
    │   │   ├── NavigationScreen.kt
    │   │   ├── SettingsScreen.kt
    │   │   └── WaitingScreen.kt
    │   │
    │   ├── sound/
    │   │   └── SoundPlayer.kt
    │   │
    │   └── theme/
    │       ├── Color.kt
    │       ├── Theme.kt
    │       └── Type.kt
    │
    └── viewmodel/
        └── NavigationViewModel.kt
```

## AI usage log

This table shows and explains the use of AI in our project, broken down by completed tasks.

| Person | Task                                                                   | AI-Tool / Model   | AI contribution                 | Location                                                                                            |
| ------ | ---------------------------------------------------------------------- | ----------------- | ------------------------------- | --------------------------------------------------------------------------------------------------- |
| Arne   | Match input from voice recognition with one destination in Temi's list | Claude Sonnet 5.5 | Complete writing of the methods | [DestinationMatching.kt](app/src/main/java/com/example/teminavigator/domain/DestinationMatching.kt) |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
|        |                                                                        |                   |                                 |                                                                                                     |
