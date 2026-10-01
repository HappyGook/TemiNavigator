package com.example.teminavigator

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
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.screens.HomeScreen
import com.example.teminavigator.ui.screens.NavigationScreen
import com.example.teminavigator.ui.screens.SettingsScreen
import com.example.teminavigator.ui.screens.WaitingScreen
import com.example.teminavigator.ui.screens.mockDestinations
import com.example.teminavigator.ui.theme.TemiNavigatorTheme
import com.example.teminavigator.viewmodel.NavigationViewModel

// Screen types
// TODO: Screen type should be controlled by viewModel ?
sealed class Screen {
    object HomeScreen : Screen()
    data class NavigationScreen(val destination: Destination) : Screen()
    object WaitingScreen : Screen()
    object SettingsScreen : Screen()

}

@Composable
fun TemiApp(
    language: AppLanguage,
    onLanguageSelected:(AppLanguage) -> Unit
){
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.HomeScreen)
    }

    when (val screen = currentScreen) {
        Screen.HomeScreen -> HomeScreen(
            destinations = mockDestinations,
            onDestinationConfirmed = { destination ->
                selectedDestination = destination
                currentScreen = Screen.NavigationScreen(destination)
            },
            onOpenSettings = { currentScreen = Screen.SettingsScreen })
        Screen.SettingsScreen -> SettingsScreen(
            currentLanguage = language,
            onLanguageChanged = onLanguageSelected,
            onBack = {currentScreen = Screen.HomeScreen},
        )
        Screen.WaitingScreen -> WaitingScreen(
            onSelectLocation = {currentScreen = Screen.HomeScreen},
            onBackToNavigation = {
                selectedDestination?.let { destination ->
                    currentScreen = Screen.NavigationScreen(destination)
                }
            }
        )
        is Screen.NavigationScreen -> NavigationScreen(
            destination = screen.destination,
            onAbort = {currentScreen = Screen.HomeScreen}
        )
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: NavigationViewModel by viewModels()

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