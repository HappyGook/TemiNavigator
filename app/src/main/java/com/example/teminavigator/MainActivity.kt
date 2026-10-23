package com.example.teminavigator

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.teminavigator.data.AdminPasswordStore
import com.example.teminavigator.data.AppSettings
import com.example.teminavigator.data.DestinationLabels
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.domain.NavSpeed
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
import com.example.teminavigator.viewmodel.SpeechNotice

@Suppress("ktlint:standard:function-naming")
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
    speechNotice: SpeechNotice?,
    onSpeechNoticeShown: (Long) -> Unit,
    onStartSpeechRecognition: () -> Unit,
    adminPasswordStore: AdminPasswordStore,
    selected: Destination?,
    onSelect: (Destination) -> Unit,
    onDeselect: () -> Unit,
    path: List<Offset>?,
    failed: Destination?,
    dismissFailure: () -> Unit,
    settings: AppSettings,
    onSoundsEnabledChanged: (Boolean) -> Unit,
    onMusicEnabledChanged: (Boolean) -> Unit,
    onVoiceOutputEnabledChanged: (Boolean) -> Unit,
    onVoiceInputEnabledChanged: (Boolean) -> Unit,
    onAutoReturnEnabledChanged: (Boolean) -> Unit,
    homeLocationId: String,
    homeRadius: Float,
    greetingsCooldownMs: Int,
    onHomeLocationIdChanged: (String) -> Unit,
    onHomeRadiusChanged: (Float) -> Unit,
    onGreetingsEnabled: (Boolean) -> Unit,
    onGreetingsCooldownChanged: (Int) -> Unit,
    map: ImageBitmap?,
    onMapReset: () -> Unit,
    onNewMapPicked: (Uri) -> Unit,
    onArrivalFailureDelayChanged: (Int) -> Unit,
    onNavigationSpeedChanged: (NavSpeed) -> Unit,
    arrivalFailureDelaySec: Int,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(speechNotice?.id) {
        val notice = speechNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = notice.message,
            duration = SnackbarDuration.Short,
        )
        onSpeechNoticeShown(notice.id)
    }

    Box(Modifier.fillMaxSize()) {
        // If destination failed, dialog shows screen-independent
        val strings = LocalStrings.current

        failed?.let { dest ->
            AlertDialog(
                onDismissRequest = dismissFailure,
                title = { Text(strings.unreachableTitle) },
                text = { Text(strings.unreachableText(dest.displayName), fontSize = 36.sp, lineHeight = 36.sp) },
                confirmButton = { Button(onClick = dismissFailure) { Text(strings.backToNavigation, fontSize = 36.sp) } },
            )
        }

        when (screen) {
            Screen.Home -> {
                HomeScreen(
                    destinations = destinations,
                    robotPose = robotPose,
                    onDestinationConfirmed = onDestinationConfirmed,
                    onOpenSettings = onOpenSettings,
                    onStartSpeechRecognition = onStartSpeechRecognition,
                    voiceInputEnabled = settings.voiceInputEnabled,
                    selectedId = selected?.id,
                    onSelect = onSelect,
                    onDeselect = onDeselect,
                    path = path,
                    map = map,
                )
            }

            Screen.Settings -> {
                SettingsScreen(
                    adminPasswordStore = adminPasswordStore,
                    arrivalFailureDelaySec = arrivalFailureDelaySec,
                    autoReturnEnabled = settings.autoReturnEnabled,
                    calibration = calibration,
                    currentLanguage = language,
                    destinations = destinations,
                    greetingsCooldownMs = greetingsCooldownMs,
                    greetingsEnabled = settings.greetingsEnabled,
                    homeLocationId = homeLocationId,
                    homeRadius = homeRadius,
                    labels = labels,
                    map = map,
                    musicEnabled = settings.musicEnabled,
                    navigationSpeed = settings.navigationSpeed,
                    onAliasesChanged = onAliasesChanged,
                    onArrivalFailureCooldownChanged = onArrivalFailureDelayChanged,
                    onAutoReturnEnabledChanged = onAutoReturnEnabledChanged,
                    onBack = onCloseSettings,
                    onCalibrationSaved = onCalibrationSaved,
                    onDisplayNameChanged = onDisplayNameChanged,
                    onGreetingsCooldownChanged = onGreetingsCooldownChanged,
                    onGreetingsEnabled = onGreetingsEnabled,
                    onHomeLocationIdChanged = onHomeLocationIdChanged,
                    onHomeRadiusChanged = onHomeRadiusChanged,
                    onLabelsReset = onLabelsReset,
                    onLanguageChanged = onLanguageSelected,
                    onMapReset = onMapReset,
                    onMusicEnabledChanged = onMusicEnabledChanged,
                    onNavigationSpeedChanged = onNavigationSpeedChanged,
                    onNewMapPicked = onNewMapPicked,
                    onReturnHomeDelayChanged = onReturnHomeDelayChanged,
                    onSoundsEnabledChanged = onSoundsEnabledChanged,
                    onVoiceInputEnabledChanged = onVoiceInputEnabledChanged,
                    onVoiceOutputEnabledChanged = onVoiceOutputEnabledChanged,
                    returnHomeDelaySec = returnHomeDelaySec,
                    soundsEnabled = settings.soundsEnabled,
                    voiceInputEnabled = settings.voiceInputEnabled,
                    voiceOutputEnabled = settings.voiceOutputEnabled,
                )
            }

            Screen.Waiting -> {
                WaitingScreen(
                    onLeaveWaiting = onLeaveWaiting,
                    returnCountdown = returnCountdown,
                )
            }

            is Screen.Navigation -> {
                NavigationScreen(
                    currentSpeed = currentSpeed,
                    destination = screen.destination,
                    map = map,
                    navigationState = navigationState,
                    onAbort = onAbort,
                    onTogglePause = onToggleNavigationPause,
                    path = path,
                    pxPerMeter = calibration.pxPerMeter,
                    robotPose = robotPose,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
        )
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: NavigationViewModel by viewModels {
        viewModelFactory {
            initializer {
                NavigationViewModel(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application,
                    TemiRobot,
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
            val calibration by viewModel.calibration.collectAsState()
            val destinations by viewModel.destinations.collectAsState()
            val failed by viewModel.failedDestination.collectAsState()
            val labels by viewModel.labels.collectAsState()
            val language by viewModel.language.collectAsState()
            val map by viewModel.mapImage.collectAsState()
            val navigationState by viewModel.navState.collectAsState()
            val path by viewModel.path.collectAsStateWithLifecycle()
            val pose = viewModel.pose.collectAsState()
            val returnCountdown by viewModel.returnCountdown.collectAsStateWithLifecycle()
            val screen by viewModel.screen.collectAsStateWithLifecycle()
            val selected by viewModel.selected.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val speechNotice by viewModel.speechNotice.collectAsStateWithLifecycle()
            val speedMps by viewModel.speedMps.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalStrings provides language.strings) {
                TemiNavigatorTheme {
                    TemiApp(
                        adminPasswordStore = adminPasswordStore,
                        arrivalFailureDelaySec = settings.arrivalFailureDelaySec,
                        calibration = calibration,
                        currentSpeed = { speedMps },
                        destinations = destinations,
                        dismissFailure = viewModel::dismissFailureDialog,
                        failed = failed,
                        greetingsCooldownMs = settings.greetingsCoolDown,
                        homeLocationId = settings.homeLocationId,
                        homeRadius = settings.homeRadius,
                        labels = labels,
                        language = language,
                        map = map,
                        navigationState = navigationState,
                        onAbort = viewModel::cancelGuidance,
                        onAliasesChanged = viewModel::setAliases,
                        onArrivalFailureDelayChanged = viewModel::setArrivalFailureDelay,
                        onAutoReturnEnabledChanged = viewModel::setAutoReturnEnabled,
                        onCalibrationSaved = viewModel::setCalibration,
                        onCloseSettings = viewModel::closeSettings,
                        onDeselect = viewModel::deselectDestination,
                        onDestinationConfirmed = viewModel::confirmDestination,
                        onDisplayNameChanged = viewModel::setDisplayName,
                        onGreetingsCooldownChanged = viewModel::setGreetingsCooldown,
                        onGreetingsEnabled = viewModel::setGreetingsEnabled,
                        onHomeLocationIdChanged = viewModel::setHomeLocationId,
                        onHomeRadiusChanged = viewModel::setHomeRadius,
                        onLabelsReset = viewModel::resetLabels,
                        onLanguageSelected = viewModel::setLanguage,
                        onLeaveWaiting = viewModel::leaveWaiting,
                        onMapReset = viewModel::resetMap,
                        onMusicEnabledChanged = viewModel::setMusicEnabled,
                        onNavigationSpeedChanged = viewModel::setNavigationSpeed,
                        onNewMapPicked = viewModel::pickMap,
                        onOpenSettings = viewModel::openSettings,
                        onReturnHomeDelayChanged = viewModel::setReturnHomeDelay,
                        onSelect = viewModel::selectDestination,
                        onSoundsEnabledChanged = viewModel::setSoundsEnabled,
                        onSpeechNoticeShown = viewModel::clearSpeechNotice,
                        onStartSpeechRecognition = viewModel::startSpeechRecognition,
                        onToggleNavigationPause = viewModel::toggleNavigationPause,
                        onVoiceInputEnabledChanged = viewModel::setVoiceInputEnabled,
                        onVoiceOutputEnabledChanged = viewModel::setVoiceOutputEnabled,
                        path = path,
                        returnCountdown = returnCountdown,
                        returnHomeDelaySec = settings.returnHomeDelaySec,
                        robotPose = { pose.value },
                        screen = screen,
                        selected = selected,
                        settings = settings,
                        speechNotice = speechNotice,
                    )
                }
            }
        }
    }
}
