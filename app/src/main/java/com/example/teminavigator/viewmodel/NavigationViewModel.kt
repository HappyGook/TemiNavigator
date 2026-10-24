package com.example.teminavigator.viewmodel
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.teminavigator.data.AppSettings
import com.example.teminavigator.data.CalibrationRepository
import com.example.teminavigator.data.DestinationLabelRepository
import com.example.teminavigator.data.DestinationLabels
import com.example.teminavigator.data.SettingsRepository
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.domain.MapLoader
import com.example.teminavigator.domain.NavSpeed
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.domain.Screen
import com.example.teminavigator.domain.SpeedCalibration
import com.example.teminavigator.domain.match
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.AppStrings
import com.example.teminavigator.ui.sound.MusicPlayer
import com.example.teminavigator.ui.sound.SoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.time.Duration.Companion.milliseconds

/**
 * A transient message for the UI following a speech-recognition result.
 *
 * [id] distinguishes successive notices with the same [message], so the UI can clear only
 * the notice it displayed.
 */
data class SpeechNotice(
    val id: Long,
    val message: String,
)

/**
 * Coordinates the app's navigation UI with the robot and its persisted settings.
 *
 * Exposes observable screen, navigation, map, destination, and robot-pose state, and translates
 * UI actions and [RobotEvent]s into robot commands. Robot coordinates are converted to map-image
 * coordinates using the current [calibration]. Destination preview commands are temporary and
 * are kept separate from active guidance.
 *
 * The robot is attached during initialization and detached when this ViewModel is cleared.
 */
class NavigationViewModel(
    app: Application,
    private val robot: RobotController,
) : AndroidViewModel(app) {
    private val soundPlayer = SoundPlayer(app)
    private val musicPlayer = MusicPlayer(app)

    // language setup
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _language =
        MutableStateFlow(
            AppLanguage.fromCode(prefs.getString("language", null)),
        )

    /** The language used for robot speech and other localized navigation messages. */
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    /** Current guidance state, including whether the robot is guiding, paused, or has arrived. */
    private val _navState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navState: StateFlow<NavigationState> = _navState.asStateFlow()

    /** Whether the robot is ready to provide navigation destinations. */
    private val _readyState = MutableStateFlow(false)
    val readyState: StateFlow<Boolean> = robot.ready

    /** Latest unhandled speech-recognition notice, or `null` when there is none. */
    private val _speechNotice = MutableStateFlow<SpeechNotice?>(null)
    val speechNotice: StateFlow<SpeechNotice?> = _speechNotice.asStateFlow()
    private var speechNoticeId = 0L

    /** Current destination of the robot */
    private var currentDestination: Destination? = null



    /** Labels used to map correct name aliases to internal destinations names   */
    private val labelRepo = DestinationLabelRepository(app)
    val labels: StateFlow<Map<String, DestinationLabels>> =
        labelRepo.labels.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Calibration used to translate robot-world coordinates (internal Temi map) into map-image pixels. */
    private val calibrationRepo = CalibrationRepository(app)
    val calibration: StateFlow<MapCalibration> =
        calibrationRepo.calibration.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MapCalibration(Offset(418F, 1191F), 24F, -1.6F), // only used until the first load finishes
        )
    // Saves a new robot-to-map calibration.
    fun setCalibration(c: MapCalibration) {
        viewModelScope.launch { calibrationRepo.save(c) }
    }

    /** Latest robot pose expressed in map-image pixels, or `null` until a pose is received. */
    private data class RawPose(
        val x: Float,
        val y: Float,
        val yaw: Float,
    )
    private val rawPose = MutableStateFlow<RawPose?>(null)

    val pose: StateFlow<RobotPose?> =
        combine(rawPose, calibration) { raw, cal ->
            raw?.let {
                val px = cal.robotToImagePx(it.x, it.y)
                RobotPose(px.x, px.y, cal.arrowRotationDeg(it.yaw))
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    /** Current robot path expressed in map-image pixels. */
    private val rawPath = MutableStateFlow<List<RobotEvent.MapPoint>>(emptyList())

    val path: StateFlow<List<Offset>> =
        combine(rawPath, calibration) { raw, cal ->
            raw.map { cal.robotToImagePx(it.x, it.y) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    /** Available robot locations, decorated with saved labels and mapped to image coordinates. */
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
                    imagePx =
                        pose?.let { (x, y, _) ->
                            cal.robotToImagePx(x.toFloat(), y.toFloat())
                        },
                    imageYawDeg =
                        pose?.let { (_, _, yaw) ->
                            cal.arrowRotationDeg(yaw.toFloat())
                        },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** selected Destination logic for the preview of the path. */
    private val _selected = MutableStateFlow<Destination?>(null)
    val selected: StateFlow<Destination?> = _selected.asStateFlow()

    // failedDestination in case temi fails to reach the destination
    private val _failedDestination = MutableStateFlow<Destination?>(null)

    // Destination from the most recent failed trip, while its failure message is shown.
    val failedDestination: StateFlow<Destination?> = _failedDestination.asStateFlow()
    private var failureJob: Job? = null

    private var previewing = false
    private var previewJob: Job? = null
    private var activeGoTo = false
    private var pendingAborts = 0

    /**
     * Selects a destination and starts a short robot path preview when navigation is idle.
     * Passing `null` clears the selection.
     */
    fun selectDestination(dest: Destination?) {
        stopPreview()
        dismissFailure()
        rawPath.value = emptyList()
        _selected.value = dest
        if (dest != null) startPreview(dest)
    }

    /** Clears the selected destination, preview path, and any active preview motion. */
    fun deselectDestination() {
        stopPreview()
        stopRobot()
        rawPath.value = emptyList()
        _selected.value = null
    }

    /** Start preview to display the path the destination, waiting for 5 sec to load the path
     *  (because the path needs to be loaded after calling the goTo function from Temi which may
     *  require some time)
     * */
    private fun startPreview(dest: Destination) {
        if (_navState.value !is NavigationState.Idle) return // never preview during real guidance
        previewing = true
        startGoingTo(dest.id)
        previewJob =
            viewModelScope.launch {
                delay(5_000.milliseconds)
                stopPreview()
            }
    }

    /** Stop preview and stop robot */
    private fun stopPreview() {
        if (!previewing) return
        previewing = false
        previewJob?.cancel()
        stopRobot()
    }

    /** Changes the app and robot speech language and persists the choice. */
    fun setLanguage(language: AppLanguage) {
        _language.value = language
        robot.setSpeechLanguage(language.code)
        prefs.edit { putString("language", language.code) }
    }

    private val strings: AppStrings
        get() = _language.value.strings

    /** Persisted application Settings. */
    private val settingsRepo = SettingsRepository(app)

    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val mapLoader = MapLoader(app)

    /** Navigation speed selected in settings */
    val speed: StateFlow<NavSpeed> =
        settings
            .map { it.navigationSpeed }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, NavSpeed.MEDIUM)

    /** Calibration values used to estimate metres per second for each navigation speed. */
    val speedCalibration: StateFlow<SpeedCalibration> = MutableStateFlow(SpeedCalibration())

    /** Estimated speed in metres per second for the currently selected navigation speed. */
    val speedMps: StateFlow<Float> =
        combine(speed, speedCalibration) { s, c -> c.mps(s) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    /** Configured map image, or `null` when the default asset should be used or loading is pending. */
    val mapImage: StateFlow<ImageBitmap?> =
        settingsRepo.settings
            .map { it.mapImagePath }
            .distinctUntilChanged()
            .map { mapLoader.load(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Imports the chosen image as the map, replacing the previously imported local map. */
    fun pickMap(uri: Uri) =
        viewModelScope.launch {
            val old = settings.first().mapImagePath
            val new = mapLoader.import(uri, old)
            if (new != null) settingsRepo.setMapImagePath(new)
        }

    /** Restores the default map and removes the previously imported local map file. */
    fun resetMap() =
        viewModelScope.launch {
            val old = settings.value.mapImagePath
            settingsRepo.setMapImagePath(null) // switch to the asset, then delete the file
            mapLoader.deleteLocal(old)
        }

    /** Seconds remaining before automatic return home, or `null` when no countdown is active. */
    private val _returnCountdown = MutableStateFlow<Int?>(null)
    val returnCountdown: StateFlow<Int?> = _returnCountdown.asStateFlow()
    private var returnJob: Job? = null


    /** Label setting functions */
    // Updates the saved display name for a robot destination.
    fun setDisplayName(
        id: String,
        name: String,
    ) {
        viewModelScope.launch { labelRepo.setDisplayName(id, name) }
    }

    // Updates the saved aliases for a robot destination. */
    fun setAliases(
        id: String,
        aliases: List<String>,
    ) {
        viewModelScope.launch { labelRepo.setAliases(id, aliases) }
    }

    // Removes a destination's saved display name and aliases. */
    fun resetLabels(id: String) {
        viewModelScope.launch { labelRepo.reset(id) }
    }

    /** Persists the selected navigation speed. */
    fun setNavigationSpeed(speed: NavSpeed) {
        viewModelScope.launch { settingsRepo.setNavigationSpeed(speed) }
    }

    /** Clears the currently displayed speech notice only if [id] is still its ID. */
    fun clearSpeechNotice(id: Long) {
        if (_speechNotice.value?.id == id) _speechNotice.value = null
    }


    /** Speech / Voice recognition */
    // Starts the speechRecognition, called when Temi hears the wakup word or the mic in HomeScreen
    // is clicked
    fun startSpeechRecognition() {
        Log.i("ViewModel", "recognition")
        _screen.value = Screen.Home // switch to Homescreen
        if (settings.value.voiceInputEnabled) startSpeechConversation()
    }

    // Calls startSpeechRecognition from the Temi Robot API
    private fun startSpeechConversation() {
        Log.i("ViewModel", "conversation")
        if (!settings.value.voiceInputEnabled) return
        robot.startSpeechRecognition()
    }

    /** Screen logic currently presented by the app. */
    // Screen currently presented by the app
    private val _screen = MutableStateFlow<Screen>(Screen.Waiting)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private var arrivalJob: Job? = null

    /** Opens the settings screen. */
    fun openSettings() {
        _screen.value = Screen.Settings
    }

    /** Closes settings and returns to the home screen. */
    fun closeSettings() {
        _screen.value = Screen.Home
    }

    /** Leaves the waiting screen, stops robot speech, and dismisses any failure message. */
    fun leaveWaiting() {
        robot.stopSpeaking()
        dismissFailure()
        _screen.value = Screen.Home
    }

    /** Sets the delay before the app automatically returns the robot to its home location. */
    fun setReturnHomeDelay(sec: Int) {
        viewModelScope.launch { settingsRepo.setReturnHomeDelay(sec) }
    }

    /** Sets how long an arrival or failure message remains before the app returns to waiting. */
    fun setArrivalFailureDelay(sec: Int) {
        viewModelScope.launch { settingsRepo.setArrivalFailureDelay(sec) }
    }

    /** Enables or disables navigation sound effects. */
    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSoundsEnabled(enabled) }
    }

    /** Enables or disables navigation music, stopping playback immediately when disabled. */
    fun setMusicEnabled(enabled: Boolean) {
        if (!enabled) musicPlayer.stop()
        viewModelScope.launch { settingsRepo.setMusicEnabled(enabled) }
    }

    /** Enables or disables spoken output, stopping current robot speech when disabled. */
    fun setVoiceOutputEnabled(enabled: Boolean) {
        if (!enabled) robot.stopSpeaking()
        viewModelScope.launch { settingsRepo.setVoiceOutputEnabled(enabled) }
    }

    /** Enables or disables speech input, stopping recognition when disabled. */
    fun setVoiceInputEnabled(enabled: Boolean) {
        if (!enabled) robot.stopSpeechRecognition()
        viewModelScope.launch { settingsRepo.setVoiceInputEnabled(enabled) }
    }

    /** Enables or disables automatic return to the configured home location. */
    fun setAutoReturnEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoReturnEnabled(enabled) }
    }

    /** Sets the robot location ID used as the automatic return destination. */
    fun setHomeLocationId(newId: String) {
        viewModelScope.launch { settingsRepo.setHomeLocationName(newId) }
    }

    /** Sets the configured home radius in map-image pixels. */
    fun setHomeRadius(pixels: Float) {
        viewModelScope.launch { settingsRepo.setHomeRadius(pixels) }
    }

    /** Enables or disables greetings while the app is on the waiting screen. */
    fun setGreetingsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setGreetingsEnabled(enabled) }
    }

    /** Sets the minimum interval between waiting-screen greetings, in milliseconds. */
    fun setGreetingsCooldown(ms: Int) {
        viewModelScope.launch { settingsRepo.setGreetingsCooldown(ms) }
    }

    /** Returns whether the latest robot pose is within the configured radius of the home location. */
    private fun isAtHome(): Boolean {
        val pose = pose.value ?: return false
        val home = destinations.value.find { it.id == settings.value.homeLocationId } ?: return false
        val radiusMeter = settings.value.homeRadius * calibration.value.pxPerMeter // multiply radius with pxPerMeter to get real metres
        val isAtHome = hypot(pose.xPx - (home.imagePx?.x ?: 0f), pose.yPx - (home.imagePx?.y ?: 0f)) < radiusMeter
        Log.i("ViewModel", "HomeCheck called, result: $isAtHome")
        return isAtHome
    }

    /**
     * Starts the waiting-screen countdown for automatic return home.
     *
     * The countdown is skipped during previews or active navigation and is cancelled if the robot
     * reaches the home radius before it expires.
     */
    private fun startReturnTimer() {
        returnJob?.cancel()
        _returnCountdown.value = null
        if (previewing || _navState.value !is NavigationState.Idle) return
        returnJob =
            viewModelScope.launch {
                val home =
                    combine(destinations, pose) { dests, p ->
                        val h = dests.find { it.id == settings.value.homeLocationId }
                        if (h?.imagePx != null && p != null) h else null
                    }.filterNotNull().first()
                if (isAtHome()) return@launch

                var left = settings.value.returnHomeDelaySec
                while (left > 0) {
                    _returnCountdown.value = left
                    delay(1_000.milliseconds)
                    if (isAtHome()) { // pose may have updated meanwhile
                        _returnCountdown.value = null
                        return@launch
                    }
                    left--
                }

                _returnCountdown.value = null
                currentDestination = home
                startGoingTo(home.id)
                _screen.value = Screen.Navigation(home) // show navigation screen when going home
            }
    }

    /** Cancels the automatic return countdown and clears its displayed value. */
    private fun cancelReturnTimer() {
        returnJob?.cancel()
        _returnCountdown.value = null
    }

    // Greetings in the waiting-screen logic.
    private var lastGreetingAt = 0L
    private var greetingIndex = 0

    /** Speaks the next greeting when waiting, idle, and past the configured cooldown. */
    private fun greetIfAppropriate() {
        if (_screen.value !is Screen.Waiting) return
        if (_navState.value !is NavigationState.Idle) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastGreetingAt < settings.value.greetingsCoolDown) return
        lastGreetingAt = now
        // rotating phrases
        speak(strings.greetings[greetingIndex++ % strings.greetings.size])
    }

    /**
     * Connects the ViewModel to robot events and keeps robot speed and return-home behavior
     * synchronized with app state.
     */
    init {
        robot.setSpeechLanguage(_language.value.code)
        robot.attach()
        viewModelScope.launch {
            robot.events.collect { event -> handleEvent(event) }
        }
        viewModelScope.launch { speed.collect { robot.setSpeed(it) } }
        viewModelScope.launch {
            combine(
                screen,
                settings.map { it.autoReturnEnabled to it.returnHomeDelaySec }.distinctUntilChanged(),
            ) { currentScreen, (autoReturnEnabled, _) ->
                currentScreen is Screen.Waiting && autoReturnEnabled
            }.collect { shouldRunTimer ->
                if (shouldRunTimer) startReturnTimer() else cancelReturnTimer()
            }
        }
    }

    /** Releases robot and audio resources when this ViewModel is cleared. */
    override fun onCleared() {
        robot.detach()
        soundPlayer.release()
        musicPlayer.stop()
    }

    /** Announces a failed destination and returns to the waiting screen after the configured delay. */
    private fun showFailure(dest: Destination) {
        _failedDestination.value = dest
        speak(strings.unreachableText(dest.displayName))
        failureJob?.cancel()
        failureJob =
            viewModelScope.launch {
                delay((settings.value.arrivalFailureDelaySec * 1000).milliseconds)
                _failedDestination.value = null
                if (_screen.value is Screen.Navigation && _navState.value is NavigationState.Idle) {
                    _screen.value = Screen.Waiting
                }
            }
    }

    /** Starts guidance to [destination] and announces the selected destination. */
    fun startGuidance(destination: Destination) {
        currentDestination = destination
        startGoingTo(destination.id)
        speak(strings.spokenStarting(destination.displayName))
    }

    /** Toggles active guidance between guiding and paused; has no effect in other states. */
    fun toggleNavigationPause() {
        val dest = currentDestination ?: return
        when (_navState.value) {
            is NavigationState.Guiding -> {
                _navState.value = NavigationState.Paused(dest)
                pauseMusicIfEnabled { pause() }
                stopRobot()
            }

            is NavigationState.Paused -> {
                _navState.value = NavigationState.Guiding(dest)
                resumeMusicIfEnabled { resume() }
                startGoingTo(dest.id)
            }

            else -> {
                return
            }
        }
    }

    /** Confirms a destination, starts guidance, and opens its navigation screen. */
    fun confirmDestination(destination: Destination) {
        stopPreview()
        dismissFailure()
        arrivalJob?.cancel()
        playSoundIfEnabled { playStart() }
        startGuidance(destination)
        _screen.value = Screen.Navigation(destination)
    }

    private fun resetNavigationSession() {
        arrivalJob?.cancel()
        currentDestination = null
        rawPath.value = emptyList()
        _navState.value = NavigationState.Idle
    }

    /** Cancels active or preview navigation and returns the app to the waiting screen. */
    fun cancelGuidance() {
        val isActiveNavigation =
            !previewing && currentDestination != null &&
                (activeGoTo || _navState.value is NavigationState.Guiding || _navState.value is NavigationState.Paused)
        if (isActiveNavigation) {
            playSoundIfEnabled { playError() }
            stopMusicIfEnabled { stop() }
        }
        stopRobot()
        resetNavigationSession()
        _screen.value = Screen.Waiting // return timer only runs here
        speak(strings.spokenAbort)
    }

    /** Speaks [text] through the robot when voice output is enabled. */
    fun speak(text: String) {
        if (settings.value.voiceOutputEnabled) robot.speak(text, _language.value.code)
    }

    /** Stops current robot movement and tracks the expected cancellation event. */
    fun stopRobot() {
        if (!activeGoTo) return // nothing moving -> no abort will come
        activeGoTo = false
        pendingAborts++
        robot.stop()
    }

    /** Sends a go-to command for a robot destination ID. */
    fun startGoingTo(id: String) {
        if (activeGoTo) pendingAborts++ // new goTo aborts the running one
        activeGoTo = true
        robot.goTo(id)
    }

    // handler for when the guidance is failed
    private fun onGuidanceFailed(dest: Destination) {
        resetNavigationSession()

        if (dest.id == settings.value.homeLocationId) {
            // failed trip home
            _screen.value = Screen.Waiting
            return
        }
        showFailure(dest)
    }

    /** Dismisses the current destination failure message. */
    fun dismissFailure() {
        failureJob?.cancel()
        _failedDestination.value = null
    }

    /** Dismisses a failure message and returns from navigation to the home screen. */
    fun dismissFailureDialog() {
        dismissFailure()
        if (_screen.value is Screen.Navigation) _screen.value = Screen.Home
    }

    /**
     * Applies a robot event to the ViewModel state and coordinates related navigation effects.
     *
     * Handles movement and arrival results, position and path updates, readiness, speech
     * recognition, wake-word detection, and greeting triggers.
     *
     * @param event Robot event to be handled, i.e. canceling Navigation
     */
    private fun handleEvent(event: RobotEvent) {
        when (event) {
            is RobotEvent.GoToCancelled -> {
                if (pendingAborts > 0) {
                    pendingAborts--
                    return
                } // expected, caused by user/system
                activeGoTo = false
                when {
                    previewing -> { // cancel happens in preview -> user tapped cancel button
                        _selected.value?.let { _selected.value = null } // remove selected -> return to list
                        stopPreview()
                    }

                    currentDestination != null && _navState.value !is NavigationState.Paused -> { // not preview & not expected -> failure
                        playSoundIfEnabled { playError() }
                        stopMusicIfEnabled { stop() }
                        onGuidanceFailed(currentDestination!!)
                    }
                }
            }

            is RobotEvent.GoToFinished -> {
                activeGoTo = false
                if (previewing) {
                    stopPreview()
                    return
                }
                currentDestination?.let { dest ->
                    playSoundIfEnabled { playArrival() }
                    stopMusicIfEnabled { stop() }
                    speak(strings.spokenArrived(dest.displayName))
                    _navState.value = NavigationState.Arrived(dest)
                    arrivalJob?.cancel()
                    arrivalJob =
                        viewModelScope.launch {
                            delay((settings.value.arrivalFailureDelaySec * 1000).milliseconds)
                            resetNavigationSession()
                            _screen.value = Screen.Waiting
                        }
                }
            }

            is RobotEvent.GoToStarted -> {
                currentDestination?.let {
                    if (!previewing) {
                        _navState.value = NavigationState.Guiding(it)
                        _selected.value = null
                        playMusicIfEnabled { play(_language.value) }
                    }
                }
            }

            is RobotEvent.PositionChanged -> {
                Log.i("ViewModel", "Viewmodel received a positionChanged event: X=${event.x}, Y=${event.y} ")
                if (event.x == 0f && event.y == 0f) return
                rawPose.value = RawPose(event.x, event.y, event.yaw)
            }

            is RobotEvent.Ready -> {
                _readyState.value = event.isReady
            }

            // When speech recognition successfully -> try and match destination
            is RobotEvent.SpeechRecognised -> {
                val destination = destinations.value.match(event.text)
                Log.i(
                    "SpeechDebug",
                    "PreviewingState: $previewing, CurrentDest $currentDestination, " +
                        "\n Dest detected $destination, Path found?: ${path.value.size > 1}, Nav state: ${navState.value} \n" +
                        "Selected value? : ${selected.value} Failed dest: ${failedDestination.value}",
                )
                if (destination != null) {
                    confirmDestination(destination)
                } else {
                    speak(strings.spokenUnknownDestination)
                    _speechNotice.value =
                        SpeechNotice(
                            id = ++speechNoticeId,
                            message = strings.spokenUnknownDestination,
                        )
                }
            }

            RobotEvent.WakeupWordDetected -> {
                if (settings.value.voiceInputEnabled) startSpeechConversation()
            }

            is RobotEvent.DetectionStatusChanged -> {
                if (event.status == RobotEvent.DetectionState.Detected) greetIfAppropriate()
            }

            is RobotEvent.PathChanged -> {
                when {
                    previewing -> {
                        rawPath.value = event.path
                        stopPreview()
                    }

                    // preview the path, then stop
                    currentDestination != null -> {
                        rawPath.value = event.path
                    } // real guidance (re-routing)
                }
            }
        }
    }

    /**
     * Functions for better readability - all of them check, if enabled in settings, if yes, play
     * associated sound or music.
     */
    private inline fun playSoundIfEnabled(play: SoundPlayer.() -> Unit) {
        if (settings.value.soundsEnabled) play(soundPlayer)
    }
    private inline fun playMusicIfEnabled(play: MusicPlayer.() -> Unit) {
        if (settings.value.musicEnabled) play(musicPlayer)
    }
    private inline fun pauseMusicIfEnabled(pause: MusicPlayer.() -> Unit) {
        if (settings.value.musicEnabled) pause(musicPlayer)
    }
    private inline fun resumeMusicIfEnabled(resume: MusicPlayer.() -> Unit) {
        if (settings.value.musicEnabled) resume(musicPlayer)
    }
    private inline fun stopMusicIfEnabled(stop: MusicPlayer.() -> Unit) {
        if (settings.value.musicEnabled) stop(musicPlayer)
    }
}
