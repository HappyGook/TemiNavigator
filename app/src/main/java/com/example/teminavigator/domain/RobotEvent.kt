package com.example.teminavigator.domain

/*
* To format the stuff robot reports to the app in one type
 */
sealed interface RobotEvent {
    data object Ready : RobotEvent
    data class GoToStarted(val locationId: String) : RobotEvent
    data class GoToFinished(val locationId: String) : RobotEvent
    data class GoToCancelled(val locationId: String, val reason: String?) : RobotEvent
    data class SpeechRecognised(val text: String) : RobotEvent
    data class PositionChanged(val x:Float, val y:Float, val diff: Float) : RobotEvent
}