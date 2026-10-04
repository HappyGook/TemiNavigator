package com.example.teminavigator.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val RobotPinBackgroundBlue = Color(0x552196F3)
val RobotPinForegroundBlue = Color(0xFF2196F3)

val PinColor = Color(0xFF6135E5)

val SelectedPinColor = Color(0xFFBE2CB7)

val ColorScheme.locationBackground: Color
    get() = RobotPinBackgroundBlue

val ColorScheme.locationForeground: Color
    get() = RobotPinForegroundBlue

val ColorScheme.pinColor: Color
    get() = PinColor

val ColorScheme.selectedPinColor: Color
    get() = SelectedPinColor