package com.example.teminavigator

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.screens.HomeScreen
import com.example.teminavigator.ui.screens.SettingsScreen
import com.example.teminavigator.ui.screens.mockDestinations
import com.example.teminavigator.ui.theme.TemiNavigatorTheme
import com.example.teminavigator.viewmodel.NavigationViewModel

// Screen types
// TODO: Screen type should be controlled by viewModel ?
sealed class Screen {
    object HomeScreen : Screen()
    class NavigationScreen(val destination: String) : Screen()
    object WaitingScreen : Screen()
    object SettingsScreen : Screen()

}

@Composable
fun TemiApp(
    language: AppLanguage,
    onLanguageSelected:(AppLanguage) -> Unit
){
    var currentScreen by remember { mutableStateOf<Screen>(Screen.HomeScreen) } // change state for testing

    when(currentScreen){
        is Screen.HomeScreen -> HomeScreen(
            destinations = mockDestinations,
            onDestinationConfirmed = { Log.i("Info", "Destination Confirmed") },
            onOpenSettings = { currentScreen = Screen.SettingsScreen })
        is Screen.SettingsScreen -> SettingsScreen(
            currentLanguage = language,
            onLanguageChanged = onLanguageSelected,
            onBack = {currentScreen = Screen.HomeScreen}
        )
        is Screen.WaitingScreen -> print("TODO")
        is Screen.NavigationScreen -> print("TODO")
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: NavigationViewModel by viewModels {
        viewModelFactory {
            initializer {
                NavigationViewModel(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application,
                    TemiRobot
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // take latest value of the language flow
            val language by viewModel.language.collectAsState()

            CompositionLocalProvider(LocalStrings provides language.strings) {
                TemiNavigatorTheme {
                    TemiApp(
                        language = language,
                        onLanguageSelected = viewModel::setLanguage
                    )
                }
            }
        }
        }
    }