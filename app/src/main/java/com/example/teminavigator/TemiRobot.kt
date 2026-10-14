package com.example.teminavigator
import android.util.Log
import com.example.teminavigator.domain.NavSpeed
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttRequest
import com.robotemi.sdk.SttLanguage
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TemiRobot: RobotController,
    OnRobotReadyListener,
    OnGoToLocationStatusChangedListener,
    OnRequestPermissionResultListener,
    OnCurrentPositionChangedListener,
    OnDetectionStateChangedListener,
    OnGoToNavPathChangedListener,
    Robot.AsrListener,
    Robot.WakeupWordListener {

    const val REQUEST_CODE_MAP = 1

    private val robot: Robot get()=Robot.getInstance()

    // private constantly changing version
    private var _ready = MutableStateFlow(false)

    // public on demand var
    var ready : StateFlow<Boolean> = _ready

    private var _events = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 10)
    override var events = _events

    private var speechLanguage = SttLanguage.SYSTEM
    private var speechLanguageCode = ""

    override val availableLocations: List<String>
        get() = robot.locations


    // funcs to subscribe / unsubscribe the robot to events from sdk
    override fun attach(){
        robot.addOnRobotReadyListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnRequestPermissionResultListener(this)
        robot.addAsrListener(this)
        robot.addWakeupWordListener(this)
        robot.addOnDetectionStateChangedListener(this)
        robot.addOnGoToNavPathChangedListener(this)
    }

    override fun detach(){
        robot.removeOnRobotReadyListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeOnRequestPermissionResultListener(this)
        robot.removeAsrListener(this)
        robot.removeWakeupWordListener(this)
        robot.removeOnDetectionStateChangedListener(this)
        robot.removeOnGoToNavPathChangedListener(this)
    }

    override fun goTo(locationId: String) = robot.goTo(locationId)
    override fun stop() = robot.stopMovement()
    override fun speak(text: String, languageCode: String) {
        robot.speak(TtsRequest.create(text, false, ttsLanguage(languageCode)))
    }

    override fun stopSpeaking() {
        robot.cancelAllTtsRequests()
    }

    override fun stopSpeechRecognition() {
        robot.finishConversation()
    }

    override fun setSpeechLanguage(languageCode: String) {
        speechLanguageCode = languageCode
        speechLanguage = when (languageCode.substringBefore('-').lowercase()) {
            "de" -> SttLanguage.DE_DE
            "en" -> SttLanguage.EN_US
            else -> SttLanguage.SYSTEM
        }
    }

    override fun startSpeechRecognition(question: String, speakPrompt: Boolean) {
        Log.i("Temi", "Starting speech conversation")
        val request = SttRequest(languages = listOf(speechLanguage))
        if (speakPrompt) {
            robot.askQuestion(
                TtsRequest.create(question, false, ttsLanguage(speechLanguageCode)),
                request
            )
        } else {
            robot.wakeup(request)
        }
    }

    private fun ttsLanguage(languageCode: String): TtsRequest.Language = when (
        languageCode.substringBefore('-').lowercase()
    ) {
        "de" -> TtsRequest.Language.DE_DE
        "en" -> TtsRequest.Language.EN_US
        "ru" -> TtsRequest.Language.RU_RU
        else -> TtsRequest.Language.SYSTEM
    }

    override fun locationPoses(): Map<String, Triple<Double, Double, Double>> {
        Log.i("Temi", "Location Poses called!")

        val map = robot.getMapData()
        if (map == null) {
            Log.i("Temi", "Robot map data was empty")
            return emptyMap()
        }

        val poses = map.locations.mapNotNull { layer ->
            val pose = layer.layerPoses?.firstOrNull() ?: return@mapNotNull null
            layer.layerId to Triple(
                pose.x.toDouble(),
                pose.y.toDouble(),
                pose.theta.toDouble()
            )
        }.toMap()

        poses.forEach { (name, pose) ->
            Log.i(
                "Temi",
                "$name: x=${pose.first}, y=${pose.second}, theta=${pose.third}"
            )
        }
        return poses
    }

    override fun getSpeed(): NavSpeed = when (robot.goToSpeed) {
        SpeedLevel.SLOW -> NavSpeed.SLOW
        SpeedLevel.HIGH -> NavSpeed.HIGH
        else -> NavSpeed.MEDIUM          // MEDIUM and anything the SDK adds later
    }

    override fun setSpeed(speed: NavSpeed) {
        robot.goToSpeed = when (speed) {
            NavSpeed.SLOW -> SpeedLevel.SLOW
            NavSpeed.MEDIUM -> SpeedLevel.MEDIUM
            NavSpeed.HIGH -> SpeedLevel.HIGH
        }
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        when (status){
            "start" -> _events.tryEmit(RobotEvent.GoToStarted(locationId=location))
            "abort" -> _events.tryEmit(RobotEvent.GoToCancelled(locationId=location, reason = description))
            "complete" -> _events.tryEmit(RobotEvent.GoToFinished(locationId=location))
        }
        val (x,y,yaw,tilt) = robot.getPosition()
        Log.i("Temi","Location Status changed: $status")
        _events.tryEmit(RobotEvent.PositionChanged(x,y,yaw))
    }

    // Is called when speech recognition is done and spoken phrase is converted to string
    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        robot.finishConversation()   // stops Temi's default NLP/answer
        if (asrResult.isNotBlank()) {
            _events.tryEmit(RobotEvent.SpeechRecognised(asrResult))
        }
    }

    override fun onWakeupWord(
        wakeupWord: String,
        direction: Int,
        origin: WakeupOrigin
    ) {
        _events.tryEmit(RobotEvent.WakeupWordDetected)
    }

    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) return
        val missing = missingPermissions()
        if (missing.isNotEmpty()) {
            Log.i("Permissions", "Requesting $missing")
            robot.requestPermissions(missing, REQUEST_CODE_MAP)
            return
        }
        // Permissions were already granted
        _events.tryEmit(RobotEvent.Ready(isReady))
        Log.i("Temi","Robot ready state: $isReady")
        val (x,y,yaw,tilt) = robot.getPosition()
        Log.i("Temi","X: $x, Y:$y, yaw: $yaw, tilt:$tilt")
        // emit positionChanged to get starting coordinates of the robot
        _events.tryEmit(RobotEvent.PositionChanged(x,y,yaw))
        robot.setDetectionModeOn(true,1.5f)
        robot.hideTopBar()
    }

    override fun onCurrentPositionChanged(position: Position) {
        Log.i("Temi","onCurrentPosChanged: X: ${position.x}, Y:${position.y}")
        _events.tryEmit(RobotEvent.PositionChanged(x = position.x, y = position.y, yaw = position.yaw))
    }

    private val requiredPermissions = listOf(Permission.MAP, Permission.SETTINGS)
    private fun missingPermissions() =
        requiredPermissions.filter { robot.checkSelfPermission(it) != Permission.GRANTED }

    override fun onRequestPermissionResult(
        permission: Permission,
        grantResult: Int,
        requestCode: Int
    ) {
        if (requestCode != REQUEST_CODE_MAP) return
        Log.i("Permissions", "$permission result: $grantResult")
        if (grantResult == Permission.GRANTED && missingPermissions().isEmpty()) {
            onRobotReady(true)
        }
    }

    override fun onDetectionStateChanged(state: Int) {
        Log.i("Temi_Detection", "Detection state changed to $state")
        val mapped = when (state) {
            OnDetectionStateChangedListener.DETECTED -> RobotEvent.DetectionState.Detected
            OnDetectionStateChangedListener.LOST -> RobotEvent.DetectionState.Lost
            else -> RobotEvent.DetectionState.Idle
        }
        _events.tryEmit(RobotEvent.DetectionStatusChanged(mapped))
    }

    override fun onGoToNavPathChanged(path: List<LayerPose>) {
        Log.i("Temi_NavPath","Neuer path berechnet: $path")
    }
}