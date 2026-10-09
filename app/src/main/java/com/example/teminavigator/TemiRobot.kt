package com.example.teminavigator
import android.util.Log
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TemiRobot: RobotController,
    OnRobotReadyListener,
    OnGoToLocationStatusChangedListener,
    OnRequestPermissionResultListener,
    OnCurrentPositionChangedListener {

    const val REQUEST_CODE_MAP = 1

    private val robot: Robot get()=Robot.getInstance()

    // private constantly changing version
    private var _ready = MutableStateFlow(false)

    // public on demand var
    var ready : StateFlow<Boolean> = _ready

    private var _events = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 10)
    override var events = _events

    override val availableLocations: List<String>
        get() = robot.locations


    // funcs to subscribe / unsubscribe the robot to events from sdk
    override fun attach(){
        robot.addOnRobotReadyListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnRequestPermissionResultListener(this)
    }

    override fun detach(){
        robot.removeOnRobotReadyListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeOnRequestPermissionResultListener(this)
    }

    override fun goTo(locationId: String) = robot.goTo(locationId)
    override fun stop() = robot.stopMovement()
    override fun speak(text: String, languageCode: String) {
        val ttsLang = when (languageCode) {
            "de" -> TtsRequest.Language.DE_DE
            "en" -> TtsRequest.Language.EN_US
            "ru" -> TtsRequest.Language.RU_RU
            else -> TtsRequest.Language.SYSTEM
        }
        robot.speak(TtsRequest.create(text, false, ttsLang))
    }
    override fun goHome() = robot.goTo("home base") //TODO: create config variable for home name

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
    }

    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) return
        val mapPermission =
            robot.checkSelfPermission(Permission.MAP)
        if (mapPermission != Permission.GRANTED) {
            Log.i("Permissions", "Requesting permission to map...")
            robot.requestPermissions(
                listOf(Permission.MAP),
                REQUEST_CODE_MAP
            )
            return
        }
        // Permission was already granted
        _events.tryEmit(RobotEvent.Ready(isReady))
        Log.i("Temi","Robot ready state: $isReady")
        val (x,y,yaw,tilt) = robot.getPosition()
        Log.i("Temi","X: $x, Y:$y, yaw: $yaw, tilt:$tilt")
        // emit positionChanged to get starting coordinates of the robot
        _events.tryEmit(RobotEvent.PositionChanged(x,y,yaw))
    }

    override fun onCurrentPositionChanged(position: Position) {
        Log.i("Temi","X: ${position.x}, Y:${position.y}")
        _events.tryEmit(RobotEvent.PositionChanged(x = position.x, y = position.y, yaw = position.yaw))
    }

    override fun onRequestPermissionResult(
        permission: Permission,
        grantResult: Int,
        requestCode: Int
    ) {
        if (requestCode != REQUEST_CODE_MAP) return
        if (permission != Permission.MAP) return
        if (grantResult == Permission.GRANTED) {
            Log.i("Permissions", "MAP permission granted")
            _events.tryEmit(RobotEvent.Ready(true))
        } else {
            Log.w("Permissions", "MAP permission denied")
        }
    }
}