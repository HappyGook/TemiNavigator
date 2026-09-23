package com.example.teminavigator
import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnRobotReadyListener

object TemiRobot: OnRobotReadyListener {
    val robot: Robot
        get()=Robot.getInstance()
    var ready = false

    init{
        robot.addOnRobotReadyListener(this)
    }

    override fun onRobotReady(isReady: Boolean){
        ready = isReady
        robot.speak(TtsRequest.create("HALLLOOOOO!!!!!"))
        Log.i("Locations", robot.locations.toString())
        robot.goTo("home")
        robot.setCurrentGoToSpeed(1.5f)
    }
}