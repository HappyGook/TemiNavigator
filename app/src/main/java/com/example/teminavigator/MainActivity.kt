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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.teminavigator.data.AdminPasswordStore
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.screens.HomeScreen
import com.example.teminavigator.ui.screens.NavigationScreen
import com.example.teminavigator.ui.screens.SettingsScreen
import com.example.teminavigator.ui.screens.WaitingScreen
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
    onLanguageSelected: (AppLanguage) -> Unit,
    destinations: List<Destination>,
    robotPose: () -> RobotPose?,
    calibration: MapCalibration,
    onCalibrationSaved: (MapCalibration) -> Unit,
    navigationState: NavigationState,
    voiceDestination: Destination?,
    onToggleNavigationPause: (Destination) -> Unit,
    onVoiceDestinationHandled: () -> Unit,
    onStartSpeechRecognition: () -> Unit,
    adminPasswordStore: AdminPasswordStore,
){
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.HomeScreen)
    }

    LaunchedEffect(voiceDestination) {
        voiceDestination?.let { destination ->
            selectedDestination = destination
            currentScreen = Screen.NavigationScreen(destination)
            onVoiceDestinationHandled()
        }
    }

    when (val screen = currentScreen) {
        Screen.HomeScreen -> HomeScreen(
            destinations = destinations,
            robotPose = robotPose,
            onDestinationConfirmed = { destination ->
                selectedDestination = destination
                currentScreen = Screen.NavigationScreen(destination)
                Log.i("Info", "Destination Confirmed")
            },
            onOpenSettings = { currentScreen = Screen.SettingsScreen },
            onStartSpeechRecognition = onStartSpeechRecognition
        )
        Screen.SettingsScreen -> SettingsScreen(
            currentLanguage = language,
            onLanguageChanged = onLanguageSelected,
            onBack = {currentScreen = Screen.HomeScreen},
            calibration = calibration,
            onCalibrationSaved = onCalibrationSaved,
            adminPasswordStore = adminPasswordStore,
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
            navigationState = navigationState,
            onTogglePause = { onToggleNavigationPause(screen.destination) },
            onAbort = {currentScreen = Screen.HomeScreen}
        )
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
        val adminPasswordStore = AdminPasswordStore(applicationContext)
        setContent {
            // take latest value of the language flow
            val language by viewModel.language.collectAsState()
            val destinations by viewModel.destinations.collectAsState()
            val pose = viewModel.pose.collectAsState()
            val calibration by viewModel.calibration.collectAsState()
            val navigationState by viewModel.navState.collectAsState()
            val voiceDestination by viewModel.voiceDestination.collectAsState()
            Log.i("Main Map Infos","Calibration state in main activity:\n" +
                    "originX= ${calibration.originPx.x}, originY= ${calibration.originPx.y}\n" +
                    "")
            Log.i("Main Map Infos","Calibration state in main activity:\n" +
                    "originX= ${calibration.originPx.x}, originY= ${calibration.originPx.y}\n" +
                    "")

            CompositionLocalProvider(LocalStrings provides language.strings) {
                TemiNavigatorTheme {
                    TemiApp(
                        language = language,
                        onLanguageSelected = viewModel::setLanguage,
                        destinations = destinations,
                        robotPose = {pose.value},
                        calibration = calibration,
                        onCalibrationSaved = viewModel::setCalibration,
                        navigationState = navigationState,
                        onToggleNavigationPause = viewModel::toggleNavigationPause,
                        voiceDestination = voiceDestination,
                        onVoiceDestinationHandled = viewModel::clearVoiceDestination,
                        onStartSpeechRecognition = viewModel::startSpeechRecognition,
                        adminPasswordStore = adminPasswordStore
                    )
                }
            }
        }
        }
    }