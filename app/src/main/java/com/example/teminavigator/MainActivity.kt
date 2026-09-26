package com.example.teminavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.teminavigator.ui.screens.HomeScreen
import com.example.teminavigator.ui.theme.TemiNavigatorTheme

// Screen types
sealed class Screen {
    object HomeScreen : Screen()
    class NavigationScreen(val destination: String) : Screen()
    object WaitingScreen : Screen()
    object SettingsScreen : Screen()

}

@Composable
fun TemiApp(){
    var currentScreen by remember { mutableStateOf<Screen>(Screen.HomeScreen) } // change state for testing

    when(currentScreen){
        is Screen.HomeScreen -> HomeScreen()
        is Screen.SettingsScreen -> print("TODO")
        is Screen.WaitingScreen -> print("TODO")
        is Screen.NavigationScreen -> print("TODO")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemiNavigatorTheme {
                TemiApp()
                }
            }
        }
    }