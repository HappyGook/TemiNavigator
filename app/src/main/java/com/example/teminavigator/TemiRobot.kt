package com.example.teminavigator
import android.util.Log
import com.example.teminavigator.domain.NavSpeed
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.SttRequest
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnGoToNavPathChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.map.LayerPose
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import com.robotemi.sdk.voice.WakeupOrigin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The Temi SDK implementation of [RobotController], as a singleton.
 *
 * This is the only place in the app that talks to the Temi SDK. Commands from
 * the ViewModel are forwarded to the SDK's [Robot] instance. The SDK reports
 * back through the listener interfaces implemented by this object, and each
 * callback is translated into a [RobotEvent] and emitted on [events], so the
 * rest of the app never sees SDK types.
 *
 * Lifecycle: call [attach] to start receiving SDK callbacks and [detach] to
 * stop. The robot is only usable once [ready] is true, which requires the SDK
 * to report ready and the needed permissions to be granted (see [onRobotReady]).
 *
 * Responsibilities, by area:
 * - **Navigation:** [goTo], [stop], speed, location poses and the planned path.
 * - **Speech output:** [speak], [stopSpeaking].
 * - **Speech input:** [startSpeechRecognition], [stopSpeechRecognition] and the
 *   wake-up word, guarded by a lock so recognition is never started twice.
 * - **Person detection:** forwarded as [RobotEvent.DetectionStatusChanged].
 */
object TemiRobot :
    RobotController,
    OnRobotReadyListener,
    OnGoToLocationStatusChangedListener,
    OnRequestPermissionResultListener,
    OnCurrentPositionChangedListener,
    OnDetectionStateChangedListener,
    OnGoToNavPathChangedListener,
    Robot.AsrListener,
    Robot.WakeupWordListener {

    /** Request code for the permission request in [onRobotReady]. Results with another code are ignored. */
    const val REQUEST_CODE_MAP = 1

    /** The SDK's robot instance. Fetched on each access instead of stored, so it is always the current one. */
    private val robot: Robot get() = Robot.getInstance()

    // Mutable internals of the public flows; only this object may change them.
    private var _ready = MutableStateFlow(false)

    /** Read-only view of readiness. Becomes true after the robot is ready and all required permissions are granted. */
    override var ready: StateFlow<Boolean> = _ready

    /**
     * Backing stream of all events. Events are emitted without suspending (`tryEmit`).
     * It buffers up to 10 events for slow collectors, and when the buffer is full
     * the oldest event is dropped.
     */
    private var _events = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override var events = _events

    /*
     * State of speech input.
     * - speechLanguage / speechLanguageCode: language used for recognition, set by setSpeechLanguage.
     * - speechRecognitionActive: true while a recognition session runs. Guarded by
     *   speechRecognitionLock, since SDK callbacks and app calls come from different threads.
     */
    private var speechLanguage = SttLanguage.SYSTEM
    private var speechLanguageCode = ""
    private val speechRecognitionLock = Any()
    private var speechRecognitionActive = false

    /** Names of all locations saved on the robot, read live from the SDK on every access. */
    override val availableLocations: List<String>
        get() = robot.locations

    /**
     * Subscribes this object to all SDK events it translates: ready state, go-to
     * status, permission results, speech recognition, wake-up word, position,
     * person detection and navigation path.
     *
     * Call [detach] with the matching lifecycle event, otherwise the callbacks keep firing.
     */
    override fun attach() {
        robot.addOnRobotReadyListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnRequestPermissionResultListener(this)
        robot.addAsrListener(this)
        robot.addWakeupWordListener(this)
        robot.addOnCurrentPositionChangedListener(this)
        robot.addOnDetectionStateChangedListener(this)
        robot.addOnGoToNavPathChangedListener(this)
    }

    /** Unsubscribes from all SDK events registered in [attach]. */
    override fun detach() {
        robot.removeOnRobotReadyListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeOnRequestPermissionResultListener(this)
        robot.removeAsrListener(this)
        robot.removeWakeupWordListener(this)
        robot.removeOnCurrentPositionChangedListener(this)
        robot.removeOnDetectionStateChangedListener(this)
        robot.removeOnGoToNavPathChangedListener(this)
    }

    /**
     * Starts driving to a saved location.
     *
     * Returns immediately. The outcome arrives as [RobotEvent.GoToStarted],
     * [RobotEvent.GoToFinished] or [RobotEvent.GoToCancelled] via [onGoToLocationStatusChanged].
     *
     * @param locationId Name of the saved location, as in [availableLocations].
     */
    override fun goTo(locationId: String) = robot.goTo(locationId)

    /** Stops the robot's movement immediately. Used for aborting and pausing. */
    override fun stop() = robot.stopMovement()

    /**
     * Speaks a text through the SDK's text-to-speech.
     *
     * The request is created as non-blocking (`false` as second argument), so the
     * function returns right away. The language is mapped by [ttsLanguage].
     *
     * @param text Text to speak.
     * @param languageCode Language code such as "de", "en" or "de-DE".
     */
    override fun speak(
        text: String,
        languageCode: String,
    ) {
        robot.speak(TtsRequest.create(text, false, ttsLanguage(languageCode)))
    }

    /** Cancels the current and all queued speech requests. */
    override fun stopSpeaking() {
        robot.cancelAllTtsRequests()
    }

    /**
     * Ends the current speech recognition session.
     *
     * Clears the active flag, so [startSpeechRecognition] can be called again, and
     * ends the SDK conversation, which also closes its listening mode.
     */
    override fun stopSpeechRecognition() {
        synchronized(speechRecognitionLock) {
            speechRecognitionActive = false
        }
        robot.finishConversation()
    }

    /**
     * Sets the language for the next speech recognition sessions.
     *
     * Only the part before a hyphen counts, so "de" and "de-DE" are equal. German
     * maps to German and English to US English. Any other language falls back to
     * the robot's system language.
     *
     * @param languageCode Language code such as "de" or "en-US".
     */
    override fun setSpeechLanguage(languageCode: String) {
        speechLanguageCode = languageCode
        speechLanguage =
            when (languageCode.substringBefore('-').lowercase()) {
                "de" -> SttLanguage.DE_DE
                "en" -> SttLanguage.EN_US
                else -> SttLanguage.SYSTEM
            }
    }

    /**
     * Starts listening for a spoken destination in the language set by [setSpeechLanguage].
     *
     * Only one session can run at a time: if one is already active, the call is
     * logged and ignored. The result arrives as [RobotEvent.SpeechRecognised] via [onAsrResult].
     *
     * @throws RuntimeException if the SDK fails to start listening. The active
     * flag is reset first, so the call can be retried.
     */
    override fun startSpeechRecognition() {
        val request = SttRequest(languages = listOf(speechLanguage))
        val shouldStart =
            synchronized(speechRecognitionLock) {
                if (speechRecognitionActive) {
                    false
                } else {
                    speechRecognitionActive = true
                    true
                }
            }
        if (!shouldStart) {
            Log.i("Temi", "Speech recognition is already active")
            return
        }

        Log.i("Temi", "Starting speech recognition")
        try {
            robot.wakeup(request)
        } catch (exception: RuntimeException) {
            synchronized(speechRecognitionLock) {
                speechRecognitionActive = false
            }
            throw exception
        }
    }

    /**
     * SDK callback: speech was recognized and converted to text.
     *
     * Ends the session, then ends the SDK conversation so the robot's own
     * language-processing and answer are suppressed. The text is emitted as
     * [RobotEvent.SpeechRecognised], unless it is blank.
     *
     * @param asrResult The recognized text.
     * @param sttLanguage The language the SDK used for recognition.
     */
    override fun onAsrResult(
        asrResult: String,
        sttLanguage: SttLanguage,
    ) {
        Log.i("TemiVoice", "ASR result: '$asrResult' (language=$sttLanguage)")
        synchronized(speechRecognitionLock) {
            speechRecognitionActive = false
        }
        robot.finishConversation() // stops Temi's default NLP/answer
        if (asrResult.isNotBlank()) {
            _events.tryEmit(RobotEvent.SpeechRecognised(asrResult))
        }
    }

    /**
     * SDK callback: the wake-up word was heard.
     *
     * Emits [RobotEvent.WakeupWordDetected], unless a recognition session is
     * already active, so a running session isn't restarted.
     */
    override fun onWakeupWord(
        wakeupWord: String,
        direction: Int,
        origin: WakeupOrigin,
    ) {
        Log.i("TemiVoice", "Wakeup word detected: '$wakeupWord' (origin=$origin)")
        val shouldEmit =
            synchronized(speechRecognitionLock) {
                !speechRecognitionActive
            }
        if (!shouldEmit) return
        _events.tryEmit(RobotEvent.WakeupWordDetected)
    }

    /**
     * Maps a language code to the SDK's text-to-speech language.
     *
     * Supports German, English (US) and Russian. Anything else uses the system language.
     */
    private fun ttsLanguage(languageCode: String): TtsRequest.Language =
        when (
            languageCode.substringBefore('-').lowercase()
        ) {
            "de" -> TtsRequest.Language.DE_DE
            "en" -> TtsRequest.Language.EN_US
            "ru" -> TtsRequest.Language.RU_RU
            else -> TtsRequest.Language.SYSTEM
        }

    /**
     * Reads the pose of every saved location from the robot's map data.
     *
     * Takes the first pose of each location layer. The result is keyed by the
     * layer id, which is the location name that [goTo] expects. Values are the
     * SDK's x, y and theta (heading).
     *
     * @return Map from location id to (x, y, yaw), or an empty map if the robot has
     * no map data yet.
     */
    override fun locationPoses(): Map<String, Triple<Double, Double, Double>> {
        Log.i("Temi", "Location Poses called!")

        val map = robot.getMapData()
        if (map == null) {
            Log.i("Temi", "Robot map data was empty")
            return emptyMap()
        }

        val poses =
            map.locations
                .mapNotNull { layer ->
                    val pose = layer.layerPoses?.firstOrNull() ?: return@mapNotNull null
                    layer.layerId to
                        Triple(
                            pose.x.toDouble(),
                            pose.y.toDouble(),
                            pose.theta.toDouble(),
                        )
                }.toMap()

        poses.forEach { (name, pose) ->
            Log.i(
                "Temi",
                "$name: x=${pose.first}, y=${pose.second}, theta=${pose.third}",
            )
        }
        return poses
    }

    /** Reads the SDK's go-to speed and maps it to [NavSpeed]. Unknown levels count as [NavSpeed.MEDIUM]. */
    override fun getSpeed(): NavSpeed =
        when (robot.goToSpeed) {
            SpeedLevel.SLOW -> NavSpeed.SLOW
            SpeedLevel.HIGH -> NavSpeed.HIGH
            else -> NavSpeed.MEDIUM // MEDIUM and anything the SDK adds later
        }

    override fun setSpeed(speed: NavSpeed) {
        robot.goToSpeed =
            when (speed) {
                NavSpeed.SLOW -> SpeedLevel.SLOW
                NavSpeed.MEDIUM -> SpeedLevel.MEDIUM
                NavSpeed.HIGH -> SpeedLevel.HIGH
            }
    }

    /**
     * SDK callback: the status of a go-to command changed.
     *
     * Maps the SDK status strings to events: "start" to [RobotEvent.GoToStarted],
     * "complete" to [RobotEvent.GoToFinished] and "abort" to
     * [RobotEvent.GoToCancelled], with the SDK's description as the reason. Other
     * statuses are ignored. After any status, the robot's current position is also
     * emitted as [RobotEvent.PositionChanged], so the map is up to date at each step.
     *
     * @param location Name of the target location.
     * @param status Status string from the SDK.
     * @param descriptionId Numeric id of the status description (unused).
     * @param description Human-readable description, used as the cancel reason.
     */
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String,
    ) {
        when (status) {
            "start" -> _events.tryEmit(RobotEvent.GoToStarted(locationId = location))
            "abort" -> _events.tryEmit(RobotEvent.GoToCancelled(locationId = location, reason = description))
            "complete" -> _events.tryEmit(RobotEvent.GoToFinished(locationId = location))
        }
        val (x, y, yaw, _) = robot.getPosition()
        Log.i("Temi", "Location Status changed: $status")
        _events.tryEmit(RobotEvent.PositionChanged(x, y, yaw))
    }

    /**
     * SDK callback: the robot's readiness changed.
     *
     * Does nothing when the robot is not ready. Otherwise it checks the permissions
     * the app needs (see [requiredPermissions]). If some are missing, they are
     * requested and the setup resumes in [onRequestPermissionResult]. Once all are
     * granted, the setup completes:
     * - [ready] becomes true and [RobotEvent.Ready] is emitted,
     * - the initial position is emitted, so the map shows the robot immediately,
     * - person detection is switched on with a range of 1.5 and the SDK's top bar is hidden.
     */
    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) return
        val missing = missingPermissions()
        if (missing.isNotEmpty()) {
            Log.i("Permissions", "Requesting $missing")
            robot.requestPermissions(missing, REQUEST_CODE_MAP)
            return
        }
        // Permissions were already granted
        _ready.value = true
        _events.tryEmit(RobotEvent.Ready(true))
        Log.i("Temi", "Robot is ready")
        val (x, y, yaw, tilt) = robot.getPosition()
        Log.i("Temi", "X: $x, Y:$y, yaw: $yaw, tilt:$tilt")
        // emit positionChanged to get starting coordinates of the robot
        _events.tryEmit(RobotEvent.PositionChanged(x, y, yaw))
        robot.setDetectionModeOn(true, 1.5f)
        robot.hideTopBar()
    }

    /** SDK callback: the robot moved. Emitted as [RobotEvent.PositionChanged]. */
    override fun onCurrentPositionChanged(position: Position) {
        Log.i("Temi", "onCurrentPosChanged: X: ${position.x}, Y:${position.y}")
        _events.tryEmit(RobotEvent.PositionChanged(x = position.x, y = position.y, yaw = position.yaw))
    }

    /** SDK permissions the app needs: map access (locations, poses) and settings. */
    private val requiredPermissions = listOf(Permission.MAP, Permission.SETTINGS)

    /** Returns the required permissions that haven't been granted yet. */
    private fun missingPermissions() = requiredPermissions.filter { robot.checkSelfPermission(it) != Permission.GRANTED }

    /**
     * SDK callback: the user answered a permission request.
     *
     * Ignores results from other requests. When a permission was granted and none
     * are missing anymore, the setup in [onRobotReady] is run again to finish it.
     */
    override fun onRequestPermissionResult(
        permission: Permission,
        grantResult: Int,
        requestCode: Int,
    ) {
        if (requestCode != REQUEST_CODE_MAP) return
        Log.i("Permissions", "$permission result: $grantResult")
        if (grantResult == Permission.GRANTED && missingPermissions().isEmpty()) {
            onRobotReady(true)
        }
    }

    /**
     * SDK callback: person detection changed.
     *
     * Maps the SDK's state constant to [RobotEvent.DetectionState]. Unknown
     * values count as [RobotEvent.DetectionState.Idle].
     */
    override fun onDetectionStateChanged(state: Int) {
        Log.i("Temi_Detection", "Detection state changed to $state")
        val mapped =
            when (state) {
                OnDetectionStateChangedListener.DETECTED -> RobotEvent.DetectionState.Detected
                OnDetectionStateChangedListener.LOST -> RobotEvent.DetectionState.Lost
                else -> RobotEvent.DetectionState.Idle
            }
        _events.tryEmit(RobotEvent.DetectionStatusChanged(mapped))
    }

    /** SDK callback: the route to the target was (re)calculated.
     * Emitted as [RobotEvent.PathChanged].
     * The points are still in robot coordinates, not map image pixels.
     * */
    override fun onGoToNavPathChanged(path: List<LayerPose>) {
        Log.i("Temi_NavPath", "Neuer path berechnet: $path")
        _events.tryEmit(RobotEvent.PathChanged(path.map { RobotEvent.MapPoint(it.x, it.y) }))
    }
}
