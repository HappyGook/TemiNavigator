package com.example.teminavigator

import android.app.Application
import android.os.Bundle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
    onLeaveWaiting: () -> Unit
){

    when (screen) {
        Screen.Home -> HomeScreen(
            destinations = destinations,
            robotPose = robotPose,
            onDestinationConfirmed = onDestinationConfirmed,
            onOpenSettings = onOpenSettings
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
            destinations = destinations
        )
        Screen.Waiting -> WaitingScreen(
            onLeaveWaiting = onLeaveWaiting
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
        setContent {
            // take latest value of the language flow
            val language by viewModel.language.collectAsState()
            val destinations by viewModel.destinations.collectAsState()
            val pose = viewModel.pose.collectAsState()
            val calibration by viewModel.calibration.collectAsState()
            val navigationState by viewModel.navState.collectAsState()
            val labels by viewModel.labels.collectAsState()
            val speedMps by viewModel.speedMps.collectAsStateWithLifecycle()
            val screen by viewModel.screen.collectAsStateWithLifecycle()
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
                        labels = labels,
                        onDisplayNameChanged = viewModel::setDisplayName,
                        onAliasesChanged = viewModel::setAliases,
                        onLabelsReset = viewModel::resetLabels,
                        onAbort = viewModel::cancelGuidance,
                        currentSpeed = { speedMps },
                        onDestinationConfirmed = viewModel::confirmDestination,
                        onOpenSettings = viewModel::openSettings,
                        onCloseSettings = viewModel::closeSettings,
                        onLeaveWaiting = viewModel::leaveWaiting
                    )
                }
            }
        }
        }
    }