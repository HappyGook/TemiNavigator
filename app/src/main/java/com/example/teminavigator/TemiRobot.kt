package com.example.teminavigator
import android.media.tv.TsRequest
import android.util.Log
import com.example.teminavigator.domain.RobotController
import com.example.teminavigator.domain.RobotEvent
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.model.Position
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TemiRobot: RobotController, OnRobotReadyListener, OnGoToLocationStatusChangedListener, OnCurrentPositionChangedListener {
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
    }

    override fun detach(){
        robot.removeOnRobotReadyListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
    }

    override fun goTo(locationId: String) = robot.goTo(locationId)
    override fun stop() = robot.stopMovement()
    override fun speak(text: String){
        robot.speak(TtsRequest.create(text,false))
    }
    // TODO: find out the exact home location name (Raum 2.72?)
    override fun goHome() = robot.goTo("home?")

    override fun locationPoses(): Map<String, Triple<Double, Double, Double>> {
        val map = robot.getMapData() ?: return emptyMap()
        return map.locations.mapNotNull { layer ->
            val pose = layer.layerPoses?.firstOrNull() ?: return@mapNotNull null
            layer.layerId to Triple(pose.x.toDouble(), pose.y.toDouble(), pose.theta.toDouble())
        }.toMap()
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        TODO("Not yet implemented")
    }

    override fun onRobotReady(isReady: Boolean){
        _events.tryEmit(RobotEvent.Ready(isReady))
        Log.i("Temi","Robot ready state: $isReady")
        val (x,y,yaw,tilt) = robot.getPosition()
        Log.i("Temi","X: $x, Y:$y, yaw: $yaw, tilt:$tilt")
        // emit positionChanged to get starting coordinates of the robot
        _events.tryEmit(RobotEvent.PositionChanged(x,y,yaw))
    }

    override fun onCurrentPositionChanged(position: Position) {
        _events.tryEmit(RobotEvent.PositionChanged(x = position.x, y = position.y, yaw = position.yaw))
    }
}