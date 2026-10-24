# Temi Navigator

## Table of Contents

1. [Overview](#overview)
2. [Settings](#settings)
    - [Admin access](#admin-access)
    - [Navigation settings](#navigation-settings)
    - [Basic settings](#basic-settings)
    - [Language](#language)
3. [Project structure](#project-structure)
4. [Architecture](#architecture)
5. [AI usage log](#ai-usage-log)


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
    ├── MainActivity.kt                  # Entry point: connects ViewModel to UI
    ├── TemiRobot.kt                     # Temi SDK integration (implements RobotController)
    │
    ├── data/                            # Local persistence (Jetpack DataStore)
    │   ├── AdminPasswordStore.kt        #   Password protecting the settings
    │   ├── CalibrationRepository.kt     #   Map calibration (origin, scale, rotation)
    │   ├── DestinationLabelRepository.kt#   Custom display names and aliases per destination
    │   ├── DestinationLabels.kt         #   Serializable format of those labels
    │   └── SettingsRepository.kt        #   All app settings with defaults
    │
    ├── domain/                          # App models and logic, independent of the UI and SDK
    │   ├── Destination.kt               #   A place the robot can guide to
    │   ├── DestinationMatching.kt       #   Fuzzy matching of spoken text to destinations
    │   ├── MapCalibration.kt            #   Robot coordinates -> map pixels
    │   ├── MapLoader.kt                 #   Loads/imports the map image, with fallback
    │   ├── NavSpeed.kt                  #   Speed levels and their real velocity (ETA)
    │   ├── NavigationState.kt           #   Idle / Guiding / Paused / Arrived
    │   ├── RobotController.kt           #   Interface the app uses to command the robot
    │   ├── RobotEvent.kt                #   Everything the robot reports back
    │   └── Screen.kt                    #   All screens the UI can show
    │
    ├── ui/                              # Jetpack Compose UI, no business logic
    │   ├── langs/                       #   Translations and language selection
    │   │   ├── AppLanguage.kt
    │   │   ├── AppStrings.kt            #     All texts of one language (one class)
    │   │   ├── EnglishStrings.kt
    │   │   ├── GermanStrings.kt
    │   │   ├── LocalStrings.kt          #     Provides the active strings to the UI
    │   │   ├── NadsatStrings.kt
    │   │   ├── PirateStrings.kt
    │   │   └── RussianStrings.kt
    │   │
    │   ├── map/                         #   Floor map
    │   │   ├── InteractiveMap.kt        #     Zoomable map with pins, robot and route
    │   │   └── MapHelper.kt             #     Coordinate conversion and drawing helpers
    │   │
    │   ├── screens/                     #   One composable per Screen
    │   │   ├── HomeScreen.kt            #     Destination selection (touch and voice)
    │   │   ├── NavigationScreen.kt      #     Guidance with route, ETA, pause and abort
    │   │   ├── SettingsScreen.kt        #     Password-protected configuration
    │   │   └── WaitingScreen.kt         #     Idle screen at the waiting position
    │   │
    │   ├── sound/
    │   │   └── SoundPlayer.kt           #   Play Sound effects
    │   │   └── MusicPlayer.kt           #   Play music via MediaPlayer
    │   │
    │   └── theme/                       #   Colors, theme and typography
    │       ├── Color.kt
    │       ├── Theme.kt
    │       └── Type.kt
    │
    └── viewmodel/
        └── NavigationViewModel.kt       # App state and logic, links robot, data and UI
```

## Architecture

The app follows MVVM with a unidirectional data flow, built with Jetpack Compose.

- View (`ui/`): Composables display state and report user actions.
  `TemiApp` is the stateless root composable. It receives everything as
  parameters, passes every action out as a callback and contains no logic.
- ViewModel (`viewmodel/`): `NavigationViewModel` is the single source of
  truth. It exposes the UI state as StateFlows (current `Screen`,
  `NavigationState`, destinations, robot pose, settings, etc.) and handles all
  user actions and robot events.
- Model (`domain/` and `data/`): `domain/` holds the app's models and logic. 
  `data/` persists settings, calibration and destination labels locally with DataStore. The
  ViewModel merges them into the UI state.

## AI usage log

This table shows and explains the use of AI in our project, broken down by completed tasks.

| Person | Task                                                                   | AI-Tool / Model                          | AI contribution                                                                                        | Location                                                                                                                                                                                                                                                                                                                                                                                            |
| ------ | ---------------------------------------------------------------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------ |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Oleg   | Create absent phrases, improve translation                             | Claude Sonnet 5.5                        | Partial to full creation of language packs                                                             | [All xStrings.kt files](app/src/main/java/com/example/teminavigator/ui/langs)                                                                                                                                                                                                                                                                                                                       |
| Oleg   | Document the package `domain`                                          | Claude Sonnet 5.5                        | Improvement / creation of KDoc-style documentation                                                     | [All files in package domain](app/src/main/java/com/example/teminavigator/domain)                                                                                                                                                                                                                                                                                                                   |
| Oleg   | Document the package `data`                                            | Claude Sonnet 5.5                        | Improvement / creation of KDoc-style documentation                                                     | [All files in package data](app/src/main/java/com/example/teminavigator/data)                                                                                                                                                                                                                                                                                                                       |
| Oleg   | Document the package `ui.map`                                          | Claude Sonnet 5.5                        | Improvement / creation of KDoc-style documentation                                                     | [All files in package ui.map](app/src/main/java/com/example/teminavigator/ui/map)                                                                                                                                                                                                                                                                                                                   |
| Oleg   | Document the package `ui.langs`                                        | Claude Sonnet 5.5                        | Improvement / creation of KDoc-style documentation                                                     | [All files in package ui.langs](app/src/main/java/com/example/teminavigator/ui/langs)                                                                                                                                                                                                                                                                                                               |
| Oleg   | Document `MainActivity.kt` `and TemiRobot.kt`                          | Claude Sonnet 5.5                        | Improvement / creation of KDoc-style documentation                                                     | [MainActivity.kt](app/src/main/java/com/example/teminavigator/MainActivity.kt) and [TemiRobot.kt](app/src/main/java/com/example/teminavigator/TemiRobot.kt)                                                                                                                                                                                                                                         |
| Arne   | Match input from voice recognition with one destination in Temi's list | Claude Sonnet 5.5                        | Complete writing of the class including the methods                                                    | [DestinationMatching.kt](app/src/main/java/com/example/teminavigator/domain/DestinationMatching.kt)                                                                                                                                                                                                                                                                                                 |
| Arne   | Creating the android UI with Jetpack compose                           | Claude Sonnet 5.5                        | Research on functions, generation of examples for orientation, improvement suggestions of written code | Package: [teminavigator/ui/screens](app/src/main/java/com/example/teminavigator/ui/screens)<br>Package: [teminavigator/ui/sound](app/src/main/java/com/example/teminavigator/ui/sound)                                                                                                                                                                                                              |
| Arne   | Securing settings with  a password                                     | Mistral Medium 3.5                       | Research on implementations and functions, research on PBKDF2-Encryption                               | [AdminPasswordStore.kt](app/src/main/java/com/example/teminavigator/data/AdminPasswordStore.kt)                                                                                                                                                                                                                                                                                                     |
| Arne   | Temi Speech Recognition                                                | Claude Sonnet 5.5                        | Research of Temi SDK, improvement and fixing of written code                                           | [TemiRobot.kt](app/src/main/java/com/example/teminavigator/TemiRobot.kt)<br>[NavigationViewModel.kt](app/src/main/java/com/example/teminavigator/viewmodel/NavigationViewModel.kt)<br>                                                                                                                                                                                                              |
| Arne   | Adding Strings for App Language                                        | Copilot gpt-6-luna                       | Generating strings for each language and adding them to the files                                      | Package: [teminavigator/ui/langs](app/src/main/java/com/example/teminavigator/ui/langs)                                                                                                                                                                                                                                                                                                             |
| Arne   | Finding and debugging errors, improving code logic                     | Copilot gpt-6-luna                       | Finding of coding errors, suggestions and implementation of solutions                                  | Major parts of  the project / complete project                                                                                                                                                                                                                                                                                                                                                      |
| Arne   | Documentation                                                          | Mistral Medium 3.5<br>Copilot gpt-6-luna | Generating pre-formulations, Formulating descriptions and explanations of code sections                | Package: [teminavigator/ui/sound](app/src/main/java/com/example/teminavigator/ui/sound)<br>Package: [teminavigator/ui/screens](app/src/main/java/com/example/teminavigator/ui/screens)<br>[NavigationViewModel.kt](app/src/main/java/com/example/teminavigator/viewmodel/NavigationViewModel.kt)<br>[AdminPasswordStore.kt](app/src/main/java/com/example/teminavigator/data/AdminPasswordStore.kt) |