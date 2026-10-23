# Temi Navigator

---

## Table of Contents

1. [Overview](#overview)
2. [Settings](#settings)
    - [Admin access](#admin-access)
    - [Basic settings](#basic-settings)
    - [Language](#language)
3. [Project structure](#project-structure)
4. 

---

## Overview

This navigator is an Android app that can be used on the [Temi v3](https://www.robotemi.com/product/temi/). 
The application is designed to guide people to destinations stored within the robot.

---

## Settings

There are several settings to adjust in the settings screen of the application.

### Admin access

The default password to access the settings of the app is by default set to ``admin``.
The default password is changeable in the code via the [AdminPasswordStore](app/src/main/java/com/example/teminavigator/data/AdminPasswordStore.kt) class.
The password is also changeable in the settings menu of the app. 

### Language

Currently, there are 5 different languages to choose from.
These can be adjusted in the settings menu without requiring an admin password.

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

---

## Project structure

```
app/
└── src/main/java/...
    ├── MainActivity.kt
    │
    ├── ui/
    │   ├── HomeScreen.kt
    │   ├── DestinationScreen.kt
    │   ├── NavigationScreen.kt
    │   └── SettingsScreen.kt
    │
    ├── temi/
    │   └── TemiRobot.kt
    │
    ├── domain/ 
    │   ├── NavigationState.kt
    │   ├── RobotController.kt
    │   ├── RobotEvent.kt
    │   └── Destination.kt
    │
    └── viewmodel/
        └── NavigationViewModel.kt
```

