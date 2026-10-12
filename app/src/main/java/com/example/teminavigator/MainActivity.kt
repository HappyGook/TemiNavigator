package com.example.teminavigator

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.teminavigator.data.AdminPasswordStore
import com.example.teminavigator.data.DestinationLabels
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.domain.Screen
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.screens.HomeScreen
import com.example.teminavigator.ui.screens.NavigationScreen
import com.example.teminavigator.ui.screens.SettingsScreen
import com.example.teminavigator.ui.screens.WaitingScreen
import com.example.teminavigator.ui.theme.TemiNavigatorTheme
import com.example.teminavigator.viewmodel.NavigationViewModel

@Composable
fun TemiApp(
    screen: Screen,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    destinations: List<Destination>,
    robotPose: () -> RobotPose?,
    calibration: MapCalibration,
    onCalibrationSaved: (MapCalibration) -> Unit,
    navigationState: NavigationState,
    onToggleNavigationPause: () -> Unit,
    labels: Map<String, DestinationLabels>,
    onDisplayNameChanged: (String, String) -> Unit,
    onAliasesChanged: (String, List<String>) -> Unit,
    onLabelsReset: (String) -> Unit,
    onAbort: () -> Unit,
    currentSpeed: () -> Float,
    onDestinationConfirmed: (Destination) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onLeaveWaiting: () -> Unit,
    returnHomeDelaySec: Int,
    onReturnHomeDelayChanged: (Int) -> Unit,
    returnCountdown: Int?,
    voiceDestination: Destination?,
    onVoiceDestinationHandled: () -> Unit,
    onStartSpeechRecognition: () -> Unit,
    adminPasswordStore: AdminPasswordStore,
){
/*
    when (screen) {
        Screen.Home -> HomeScreen(
    LaunchedEffect(voiceDestination) {
        voiceDestination?.let { destination ->
            selectedDestination = destination
            currentScreen = Screen.NavigationScreen(destination)
            onVoiceDestinationHandled()
        }
    }
*/
    when (screen) {
        Screen.Home -> HomeScreen(
            destinations = destinations,
            robotPose = robotPose,
            onDestinationConfirmed = onDestinationConfirmed,
            onOpenSettings = onOpenSettings,
            onStartSpeechRecognition = onStartSpeechRecognition
        )
        Screen.Settings -> SettingsScreen(
            currentLanguage = language,
            onLanguageChanged = onLanguageSelected,
            onBack = onCloseSettings,
            calibration = calibration,
            onCalibrationSaved = onCalibrationSaved,
            labels = labels,
            onDisplayNameChanged = onDisplayNameChanged,
            onAliasesChanged = onAliasesChanged,
            onLabelsReset = onLabelsReset,
            destinations = destinations,
            returnHomeDelaySec = returnHomeDelaySec,
            onReturnHomeDelayChanged = onReturnHomeDelayChanged,
            adminPasswordStore = adminPasswordStore,
        )
        Screen.Waiting -> WaitingScreen(
            onLeaveWaiting = onLeaveWaiting,
            returnCountdown = returnCountdown
        )
        is Screen.Navigation -> NavigationScreen(
            destination = screen.destination,
            robotPose = robotPose,
            currentSpeed = currentSpeed,
            pxPerMeter = calibration.pxPerMeter,
            navigationState = navigationState,
            onTogglePause = onToggleNavigationPause,
            onAbort = onAbort
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
            val labels by viewModel.labels.collectAsState()
            val voiceDestination by viewModel.voiceDestination.collectAsState()
            val speedMps by viewModel.speedMps.collectAsStateWithLifecycle()
            val screen by viewModel.screen.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val returnCountdown by viewModel.returnCountdown.collectAsStateWithLifecycle()
            /*
            Log.i("Main Map Infos","Calibration state in main activity:\n" +
                    "originX= ${calibration.originPx.x}, originY= ${calibration.originPx.y}\n" +
                    "")
            Log.i("Main Map Infos","Calibration state in main activity:\n" +
                    "originX= ${calibration.originPx.x}, originY= ${calibration.originPx.y}\n" +
                    "")
             */

            CompositionLocalProvider(LocalStrings provides language.strings) {
                TemiNavigatorTheme {
                    TemiApp(
                        screen = screen,
                        language = language,
                        onLanguageSelected = viewModel::setLanguage,
                        destinations = destinations,
                        robotPose = { pose.value },
                        calibration = calibration,
                        onCalibrationSaved = viewModel::setCalibration,
                        navigationState = navigationState,
                        onToggleNavigationPause = viewModel::toggleNavigationPause,
                        voiceDestination = voiceDestination,
                        onVoiceDestinationHandled = viewModel::clearVoiceDestination,
                        onStartSpeechRecognition = viewModel::startSpeechRecognition,
                        adminPasswordStore = adminPasswordStore,
                        labels = labels,
                        onDisplayNameChanged = viewModel::setDisplayName,
                        onAliasesChanged = viewModel::setAliases,
                        onLabelsReset = viewModel::resetLabels,
                        onAbort = viewModel::cancelGuidance,
                        currentSpeed = { speedMps },
                        onDestinationConfirmed = viewModel::confirmDestination,
                        onOpenSettings = viewModel::openSettings,
                        onCloseSettings = viewModel::closeSettings,
                        onLeaveWaiting = viewModel::leaveWaiting,
                        returnHomeDelaySec = settings.returnHomeDelaySec,
                        onReturnHomeDelayChanged = viewModel::setReturnHomeDelay,
                        returnCountdown = returnCountdown
                    )
                }
            }
        }
        }
    }