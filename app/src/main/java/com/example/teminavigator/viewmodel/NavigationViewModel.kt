package com.example.teminavigator.viewmodel
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
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
import com.example.teminavigator.domain.MapLoader
import com.example.teminavigator.domain.NavSpeed
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.domain.Screen
import com.example.teminavigator.domain.SpeedCalibration
import com.example.teminavigator.domain.match
import com.example.teminavigator.ui.langs.AppStrings
import com.example.teminavigator.ui.sound.MusicPlayer
import com.example.teminavigator.ui.sound.SoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.time.Duration.Companion.milliseconds

data class SpeechNotice(val id: Long, val message: String)

class NavigationViewModel(
    app: Application,
    private val robot: RobotController
) : AndroidViewModel(app){
    private val soundPlayer = SoundPlayer(app)
    private val musicPlayer = MusicPlayer(app)

    // language setup
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("language",null))
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _navState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navState : StateFlow<NavigationState> = _navState.asStateFlow()

    private val _readyState = MutableStateFlow(false)
    val readyState : StateFlow<Boolean> = robot.ready

    private val _speechNotice = MutableStateFlow<SpeechNotice?>(null)
    val speechNotice: StateFlow<SpeechNotice?> = _speechNotice.asStateFlow()
    private var speechNoticeId = 0L

    private var currentDestination : Destination? = null

    private val calibrationRepo = CalibrationRepository(app)

    private val labelRepo = DestinationLabelRepository(app)

    val labels: StateFlow<Map<String, DestinationLabels>> =
        labelRepo.labels.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

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

    // path transformation
    private val rawPath = MutableStateFlow<List<RobotEvent.MapPoint>>(emptyList())

    val path: StateFlow<List<Offset>> =
        combine(rawPath, calibration) { raw, cal ->
            raw.map { cal.robotToImagePx(it.x, it.y) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    // selected Destination logic for the preview of the path
    private val _selected = MutableStateFlow<Destination?>(null)
    val selected: StateFlow<Destination?> = _selected.asStateFlow()

    // failedDestination in case temi fails to reach the destination
    private val _failedDestination = MutableStateFlow<Destination?>(null)
    val failedDestination: StateFlow<Destination?> = _failedDestination.asStateFlow()
    private var failureJob: Job? = null

    private var previewing = false
    private var previewJob: Job? = null
    private var activeGoTo = false
    private var pendingAborts = 0

    fun selectDestination(dest: Destination?) {
        stopPreview()
        dismissFailure()
        rawPath.value = emptyList()
        _selected.value = dest
        if (dest != null) startPreview(dest)
    }

    fun deselectDestination() {
        stopPreview()
        stopRobot()
        rawPath.value = emptyList()
        _selected.value = null
    }

    private fun startPreview(dest: Destination) {
        if (_navState.value !is NavigationState.Idle) return   // never preview during real guidance
        previewing = true
        startGoingTo(dest.id)
        previewJob = viewModelScope.launch {
            delay(5_000.milliseconds)
            stopPreview()
        }
    }

    private fun stopPreview() {
        if (!previewing) return
        previewing = false
        previewJob?.cancel()
        stopRobot()
    }

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
    private val mapLoader = MapLoader(app)

    val speed: StateFlow<NavSpeed> = settings
        .map { it.navigationSpeed }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, NavSpeed.MEDIUM)

    val speedCalibration: StateFlow<SpeedCalibration> = MutableStateFlow(SpeedCalibration())

    val speedMps: StateFlow<Float> =
        combine(speed, speedCalibration) { s, c -> c.mps(s) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    val mapImage: StateFlow<ImageBitmap?> = settingsRepo.settings
        .map { it.mapImagePath }
        .distinctUntilChanged()
        .map { mapLoader.load(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun pickMap(uri: Uri) = viewModelScope.launch {
        val old = settings.first().mapImagePath
        val new = mapLoader.import(uri, old)
        if (new != null) settingsRepo.setMapImagePath(new) // TODO: else show error msg
    }

    fun resetMap() = viewModelScope.launch {
        val old = settings.value.mapImagePath
        settingsRepo.setMapImagePath(null) // switch to the asset, then delete the file
        mapLoader.deleteLocal(old)
    }

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

    fun setNavigationSpeed(speed: NavSpeed) {
        viewModelScope.launch { settingsRepo.setNavigationSpeed(speed) }
    }

    fun clearSpeechNotice(id: Long) {
        if (_speechNotice.value?.id == id) _speechNotice.value = null
    }

    fun resetLabels(id: String) {
        viewModelScope.launch { labelRepo.reset(id) }
    }

    fun startSpeechRecognition() {
        Log.i("ViewModel", "recognition")
        _screen.value = Screen.Home // switch to Homescreen
        if (settings.value.voiceInputEnabled) startSpeechConversation()
    }

    private fun startSpeechConversation() {
        Log.i("ViewModel", "conversation")
        if (!settings.value.voiceInputEnabled) return
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
        dismissFailure()
        _screen.value = Screen.Home
    }

    fun setReturnHomeDelay(sec: Int) {
        viewModelScope.launch { settingsRepo.setReturnHomeDelay(sec) }
    }

    fun setArrivalFailureDelay(sec: Int) {
        viewModelScope.launch { settingsRepo.setArrivalFailureDelay(sec) }
    }

    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSoundsEnabled(enabled) }
    }

    fun setVoiceOutputEnabled(enabled: Boolean) {
        if (!enabled) robot.stopSpeaking()
        viewModelScope.launch { settingsRepo.setVoiceOutputEnabled(enabled) }
    }

    fun setVoiceInputEnabled(enabled: Boolean) {
        if (!enabled) robot.stopSpeechRecognition()
        viewModelScope.launch { settingsRepo.setVoiceInputEnabled(enabled) }
    }

    fun setAutoReturnEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoReturnEnabled(enabled) }
    }

    fun setHomeLocationId(newId: String){
        viewModelScope.launch {settingsRepo.setHomeLocationName(newId)}
    }

    fun setHomeRadius(pixels: Float) {
        viewModelScope.launch {settingsRepo.setHomeRadius(pixels)}
    }

    fun setGreetingsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setGreetingsEnabled(enabled) }
    }

    fun setGreetingsCooldown(ms: Int) {
        viewModelScope.launch { settingsRepo.setGreetingsCooldown(ms) }
    }

    private fun isAtHome(): Boolean {
        val pose = pose.value ?: return false
        val home = destinations.value.find { it.id == settings.value.homeLocationId } ?: return false
        val radiusMeter = settings.value.homeRadius*calibration.value.pxPerMeter // multiply radius with pxPerMeter to get real metres
        val isAtHome = hypot(pose.xPx - (home.imagePx?.x ?: 0f), pose.yPx - (home.imagePx?.y ?: 0f)) < radiusMeter
        Log.i("ViewModel","HomeCheck called, result: $isAtHome")
        return isAtHome
    }

    private fun startReturnTimer() {
        returnJob?.cancel()
        _returnCountdown.value = null
        if (previewing || _navState.value !is NavigationState.Idle) return
        returnJob = viewModelScope.launch {
            if (isAtHome()) return@launch
            var left = settings.value.returnHomeDelaySec
            while (left > 0) {
                _returnCountdown.value = left
                delay(1_000.milliseconds)
                left--
            }

            val home = destinations.value.find { it.id == settings.value.homeLocationId }
                ?: return@launch // if home location not available - stay on waiting screen

            _returnCountdown.value = null
            currentDestination = home
            startGoingTo(home.id)
            _screen.value = Screen.Navigation(home) // show navigation screen when going home
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
        if (now - lastGreetingAt < settings.value.greetingsCoolDown) return
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
            combine(
                screen,
                settings.map { it.autoReturnEnabled to it.returnHomeDelaySec }.distinctUntilChanged()
            ) { currentScreen, (autoReturnEnabled, _) ->
                currentScreen is Screen.Waiting && autoReturnEnabled
            }.collect { shouldRunTimer ->
                if (shouldRunTimer) startReturnTimer() else cancelReturnTimer()
            }
        }
    }

    override fun onCleared() {
        robot.detach()
        soundPlayer.release()
        super.onCleared()
    }

    private fun showFailure(dest: Destination, thenWaiting: Boolean = false) {
        _failedDestination.value = dest
        speak(strings.unreachableText(dest.displayName))
        failureJob?.cancel()
        failureJob = viewModelScope.launch {
            delay((settings.value.arrivalFailureDelaySec*1000).milliseconds)
            _failedDestination.value = null
            if (thenWaiting && _screen.value is Screen.Navigation && _navState.value is NavigationState.Idle) {
                _screen.value = Screen.Waiting
            }
        }
    }

    fun startGuidance(destination: Destination){
        currentDestination = destination
        startGoingTo(destination.id)
        speak(strings.spokenStarting(destination.displayName))
    }

    fun toggleNavigationPause() {
        val dest = currentDestination ?: return
        when (_navState.value) {
            is NavigationState.Guiding -> {
                _navState.value = NavigationState.Paused(dest)
                //playMusicIfEnabled { pause() } TODO
                musicPlayer.pause()
                stopRobot()
            }
            is NavigationState.Paused -> {
                _navState.value = NavigationState.Guiding(dest)
                // playMusicIfEnabled { resume() } TODO
                musicPlayer.resume()
                startGoingTo(dest.id)
            }
            else -> Unit
        }
    }

    private fun pauseNavigation() {
        stopRobot()
    }

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

    fun cancelGuidance(){
        val isActiveNavigation = !previewing && currentDestination != null &&
            (activeGoTo || _navState.value is NavigationState.Guiding || _navState.value is NavigationState.Paused)
        if (isActiveNavigation) playSoundIfEnabled { playError() }
        stopRobot()
        resetNavigationSession()
        _screen.value = Screen.Waiting       // return timer only runs here
        speak(strings.spokenAbort)
    }

    fun speak(text: String) {
        if (settings.value.voiceOutputEnabled) robot.speak(text, _language.value.code)
    }

    fun stopRobot(){
        if (!activeGoTo) return              // nothing moving -> no abort will come
        activeGoTo = false
        pendingAborts++
        robot.stop()
    }

    fun startGoingTo(id: String){
        if (activeGoTo) pendingAborts++      // new goTo aborts the running one
        activeGoTo = true                      // TODO: verify in logs
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
        showFailure(dest, thenWaiting = true)
    }

    fun dismissFailure() {
        failureJob?.cancel()
        _failedDestination.value = null
    }

    fun dismissFailureDialog() {
        dismissFailure()
        if (_screen.value is Screen.Navigation) _screen.value = Screen.Home
    }

    private fun handleEvent(event: RobotEvent){
        when (event){
            is RobotEvent.GoToCancelled -> {
                if (pendingAborts > 0) { pendingAborts--; return }   // expected, caused by user/system
                activeGoTo = false
                when {
                    previewing -> {                                  // check preview first
                        _selected.value?.let { showFailure(it) }
                        stopPreview()
                    }
                    currentDestination != null && _navState.value !is NavigationState.Paused -> { // not preview & not expected -> failure
                        playSoundIfEnabled { playError() }
                        //playMusicIfEnabled { stop() } TODO
                        musicPlayer.stop()
                        onGuidanceFailed(currentDestination!!)
                    }
                }
            }
            is RobotEvent.GoToFinished -> {
                activeGoTo = false
                if (previewing) { stopPreview(); return }
                currentDestination?.let { dest ->
                    playSoundIfEnabled { playArrival() }
                    // playMusicIfEnabled { stop() } TODO
                    musicPlayer.stop()
                    speak(strings.spokenArrived(dest.displayName))
                    _navState.value = NavigationState.Arrived(dest)
                    arrivalJob?.cancel()
                    arrivalJob = viewModelScope.launch {
                        delay((settings.value.arrivalFailureDelaySec*1000).milliseconds)
                        resetNavigationSession()
                        _screen.value = Screen.Waiting
                    }
                }
            }
            is RobotEvent.GoToStarted -> {
                currentDestination?.let {
                    if(!previewing) {
                        _navState.value = NavigationState.Guiding(it)
                        _selected.value = null
                        // playMusicIfEnabled { play() } TODO
                        musicPlayer.play()
                    }
                }
            }
            is RobotEvent.PositionChanged -> {
                Log.i("ViewModel","Viewmodel received a positionChanged event: X=${event.x}, Y=${event.y} ")
                rawPose.value = RawPose(event.x, event.y, event.yaw)
            }
            is RobotEvent.Ready -> _readyState.value = event.isReady

            // When speech recognition successfully -> try and match destination
            is RobotEvent.SpeechRecognised -> {
                val destination = destinations.value.match(event.text)
                Log.i("SpeechDebug","PreviewingState: ${previewing}, CurrentDest ${currentDestination}, " +
                        "\n Dest detected $destination, Path found?: ${path.value.size>1}, Nav state: ${navState.value} \n" +
                        "Selected value? : ${selected.value} Failed dest: ${failedDestination.value}")
                if (destination != null) {
                    confirmDestination(destination)
                } else {
                    speak(strings.spokenUnknownDestination)
                    _speechNotice.value = SpeechNotice(
                        id = ++speechNoticeId,
                        message = strings.spokenUnknownDestination
                    )
                }
            }

            RobotEvent.WakeupWordDetected -> {
                if (settings.value.voiceInputEnabled) startSpeechConversation()
            }

            is RobotEvent.DetectionStatusChanged -> {
                if (event.status == RobotEvent.DetectionState.Detected) greetIfAppropriate()
            }

            is RobotEvent.PathChanged -> when {
                previewing -> { rawPath.value = event.path; stopPreview() }   // preview the path, then stop
                currentDestination != null -> rawPath.value = event.path      // real guidance (re-routing)
            }
        }
    }

    private inline fun playSoundIfEnabled(play: SoundPlayer.() -> Unit) {
        if (settings.value.soundsEnabled) soundPlayer.play()
    }
    private inline fun playMusicIfEnabled(play: MusicPlayer.() -> Unit) {
        //if (settings.value.musicEnabled) musicPlayer.play() TODO: add music settings
        if (settings.value.soundsEnabled) musicPlayer.play()
    }
}