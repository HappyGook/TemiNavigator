package com.example.teminavigator.viewmodel
import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.example.teminavigator.ui.langs.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import com.example.teminavigator.data.AppSettings
import com.example.teminavigator.data.CalibrationRepository
import com.example.teminavigator.data.DestinationLabelRepository
import com.example.teminavigator.data.DestinationLabels
import com.example.teminavigator.data.SettingsRepository
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.domain.NavSpeed
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.domain.Screen
import com.example.teminavigator.domain.SpeedCalibration
import com.example.teminavigator.domain.match
import com.example.teminavigator.ui.langs.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.time.Duration.Companion.milliseconds

// constants
private const val HOME_LOCATION = "home base"
private const val HOME_RADIUS_M = 0.4
private const val GREETING_COOLDOWN_MS = 10 * 1000

class NavigationViewModel(
    app: Application,
    private val robot: RobotController
) : AndroidViewModel(app){
    // language setup
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("language",null))
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _navState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navState : StateFlow<NavigationState> = _navState.asStateFlow()

    private val _voiceDestination = MutableStateFlow<Destination?>(null)
    val voiceDestination: StateFlow<Destination?> = _voiceDestination.asStateFlow()

    private val _readyState = MutableStateFlow(true)
    val readyState : StateFlow<Boolean> = _readyState.asStateFlow()

    private var currentDestination : Destination? = null

    private val calibrationRepo = CalibrationRepository(app)

    private val labelRepo = DestinationLabelRepository(app)

    val labels: StateFlow<Map<String, DestinationLabels>> =
        labelRepo.labels.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val speed: StateFlow<NavSpeed> = MutableStateFlow(NavSpeed.MEDIUM)

    val speedCalibration: StateFlow<SpeedCalibration> = MutableStateFlow(SpeedCalibration())

    val speedMps: StateFlow<Float> =
        combine(speed, speedCalibration) { s, c -> c.mps(s) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    val calibration: StateFlow<MapCalibration> =
        calibrationRepo.calibration.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MapCalibration(Offset(418F, 1191F), 24F, -1.6F)   // only used until the first load finishes
        )

    private data class RawPose(val x: Float, val y: Float, val yaw: Float)
    private val rawPose = MutableStateFlow<RawPose?>(null)
    val pose: StateFlow<RobotPose?> =
        combine(rawPose, calibration) { raw, cal ->
            raw?.let {
                val px = cal.robotToImagePx(it.x, it.y)
                RobotPose(px.x, px.y, cal.arrowRotationDeg(it.yaw))
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setCalibration(c: MapCalibration) {
        viewModelScope.launch { calibrationRepo.save(c) }
    }

    val destinations: StateFlow<List<Destination>> =
        combine(readyState, calibration, labelRepo.labels) { ready, cal, labels ->
            Log.i("Dest", "recompute: ready=$ready, origin=${cal.originPx}, scale=${cal.pxPerMeter}")
            if (!ready) return@combine emptyList()

            val poses = withContext(Dispatchers.IO) { robot.locationPoses() }
            robot.availableLocations.map { name ->
                val pose = poses[name]
                val label = labels[name]
                Destination(
                    id = name,
                    displayName = label?.displayName ?: name,
                    aliases = label?.aliases.orEmpty(),
                    imagePx = pose?.let { (x, y, _) ->
                        cal.robotToImagePx(x.toFloat(), y.toFloat())
                    },
                    imageYawDeg = pose?.let { (_, _, yaw) ->
                        cal.arrowRotationDeg(yaw.toFloat())
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setLanguage(language: AppLanguage){
        _language.value = language
        robot.setSpeechLanguage(language.code)
        prefs.edit { putString("language", language.code) }
    }

    private val strings: AppStrings
        get() = _language.value.strings

    // Settings
    private val settingsRepo = SettingsRepository(app)
    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _returnCountdown = MutableStateFlow<Int?>(null)
    val returnCountdown: StateFlow<Int?> = _returnCountdown.asStateFlow()
    private var returnJob: Job? = null

    // Label setting functions
    fun setDisplayName(id: String, name: String) {
        viewModelScope.launch { labelRepo.setDisplayName(id, name) }
    }

    fun setAliases(id: String, aliases: List<String>) {
        viewModelScope.launch { labelRepo.setAliases(id, aliases) }
    }

    fun resetLabels(id: String) {
        viewModelScope.launch { labelRepo.reset(id) }
    }

    fun clearVoiceDestination() {
        _voiceDestination.value = null
    }

    fun startSpeechRecognition() {
        robot.startSpeechRecognition()
    }

    // screen logic
    private val _screen = MutableStateFlow<Screen>(Screen.Waiting)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private var arrivalJob: Job? = null

    fun openSettings() { _screen.value = Screen.Settings }
    fun closeSettings() { _screen.value = Screen.Home }
    fun leaveWaiting() {
        robot.stopSpeaking()
        _screen.value = Screen.Home
    }

    fun setReturnHomeDelay(sec: Int) {
        viewModelScope.launch { settingsRepo.setReturnHomeDelay(sec) }
    }
    private fun isAtHome(): Boolean {
        val pose = pose.value ?: return false
        val home = destinations.value.find { it.id == HOME_LOCATION } ?: return false
        val isAtHome = hypot(pose.xPx - (home.imagePx?.x ?: 0f), pose.yPx - (home.imagePx?.y ?: 0f)) < HOME_RADIUS_M
        Log.i("ViewModel","HomeCheck called, result: $isAtHome")
        return isAtHome
    }

    private fun startReturnTimer() {
        returnJob?.cancel()
        returnJob = viewModelScope.launch {
            if (isAtHome()) return@launch
            var left = settings.value.returnHomeDelaySec
            while (left > 0) {
                _returnCountdown.value = left
                delay(1_000.milliseconds)
                left--
            }
            _returnCountdown.value = null
            currentDestination = null
            robot.goTo(HOME_LOCATION)
        }
    }

    private fun cancelReturnTimer() {
        returnJob?.cancel()
        _returnCountdown.value = null
    }

    // greetings in WaitingScreen logic
    private var lastGreetingAt = 0L
    private var greetingIndex = 0

    private fun greetIfAppropriate() {
        if (_screen.value !is Screen.Waiting) return
        if (_navState.value !is NavigationState.Idle) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastGreetingAt < GREETING_COOLDOWN_MS) return
        lastGreetingAt = now
        // rotating phrases
        speak(strings.greetings[greetingIndex++ % strings.greetings.size])
    }

    // robot integration
    init {
        robot.setSpeechLanguage(_language.value.code)
        robot.attach()
        viewModelScope.launch {
            robot.events.collect {event -> handleEvent(event)}
        }
        viewModelScope.launch { speed.collect { robot.setSpeed(it) } }
        viewModelScope.launch {
            screen.collect { if (it is Screen.Waiting) startReturnTimer() else cancelReturnTimer() }
        }
        viewModelScope.launch { speed.collect { robot.setSpeed(it) } }
        viewModelScope.launch {
            screen.collect { if (it is Screen.Waiting) startReturnTimer() else cancelReturnTimer() }
        }
    }

    override fun onCleared() {
        robot.detach()
        super.onCleared()
    }

    fun startGuidance(destination: Destination){
        currentDestination = destination
        robot.goTo(destination.id)
        speak(strings.spokenStarting(destination.displayName))
    }

    fun toggleNavigationPause() {
        val dest = currentDestination ?: return
        when (_navState.value) {
            is NavigationState.Guiding -> {
                _navState.value = NavigationState.Paused(dest)
                robot.stop()
            }
            is NavigationState.Paused -> {
                _navState.value = NavigationState.Guiding(dest)
                robot.goTo(dest.id)
            }
            else -> Unit
        }
    }

    fun confirmDestination(destination: Destination) {
        arrivalJob?.cancel()
        startGuidance(destination)
        _screen.value = Screen.Navigation(destination)
    }

    fun cancelGuidance(){
        arrivalJob?.cancel()
        _navState.value = NavigationState.Idle
        _screen.value = Screen.Home
        robot.stop()
        speak(strings.spokenAbort)
    }

    fun speak(text: String) {
        robot.speak(text, _language.value.code)
    }

    private fun handleEvent(event: RobotEvent){
        when (event){
            is RobotEvent.GoToCancelled -> {
                if (_navState.value !is NavigationState.Paused) {
                    _navState.value = NavigationState.Idle
                }
            }
            is RobotEvent.GoToFinished -> currentDestination?.let { dest ->
                _navState.value = NavigationState.Arrived(dest)
                arrivalJob?.cancel()
                arrivalJob = viewModelScope.launch { // TODO: delay e.g. 3_000L, so the user sees "arrived"
                    _navState.value = NavigationState.Idle
                    _screen.value = Screen.Waiting
                }
            }
            is RobotEvent.GoToStarted -> currentDestination?.let {
                _navState.value = NavigationState.Guiding(it)
            }
            is RobotEvent.PositionChanged -> {
                Log.i("ViewModel","Viewmodel received a positionChanged event: X=${event.x}, Y=${event.y} ")
                rawPose.value = RawPose(event.x, event.y, event.yaw)
            }
            is RobotEvent.Ready -> _readyState.value = event.isReady
            is RobotEvent.SpeechRecognised -> {
                destinations.value.match(event.text)?.let { destination ->
                    startGuidance(destination)
                    _voiceDestination.value = destination
                }
            }

            is RobotEvent.DetectionStatusChanged -> {
                if (event.status == RobotEvent.DetectionState.Detected) greetIfAppropriate()
            }
        }
    }

}