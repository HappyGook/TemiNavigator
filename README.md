# Temi Navigator

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