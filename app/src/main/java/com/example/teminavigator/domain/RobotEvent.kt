package com.example.teminavigator.domain

/**
 * Everything the robot reports back to the app, packed into one type.
 *
 * The robot layer (see [RobotController.events]) emits these events and the
 * ViewModel collects and handles them, for example by updating the
 * [NavigationState]. This keeps all Temi SDK callbacks out of the UI and
 * ViewModel code.
 */
sealed interface RobotEvent {

    /**
     * The robot's readiness changed.
     *
     * @property isReady True once the robot is ready to receive commands.
     */
    data class Ready(val isReady: Boolean): RobotEvent

    /**
     * The robot started driving to a location.
     *
     * @property locationId Temi id of the target location.
     */
    data class GoToStarted(val locationId: String) : RobotEvent

    /**
     * The robot reached a location.
     *
     * @property locationId Temi id of the location reached.
     */
    data class GoToFinished(val locationId: String) : RobotEvent

    /**
     * A drive to a location ended without arriving, for example because the user
     * stopped it or the target was unreachable.
     *
     * @property locationId Temi id of the location that was not reached.
     * @property reason Reason reported by the Temi SDK, or null if none was given.
     */
    data class GoToCancelled(val locationId: String, val reason: String?) : RobotEvent

    /** The robot's wake-up word was heard, so speech input can begin. */
    data object WakeupWordDetected : RobotEvent

    /**
     * Speech input was recognised.
     *
     * @property text The recognised text, e.g. a spoken destination name.
     */
    data class SpeechRecognised(val text: String) : RobotEvent

    /**
     * The robot's position on the map changed.
     *
     * @property x X coordinate in the robot's map coordinate system.
     * @property y Y coordinate in the robot's map coordinate system.
     * @property yaw Heading of the robot (radians).
     */
    data class PositionChanged(val x:Float, val y:Float, val yaw: Float) : RobotEvent

    /**
     * Person-detection states reported by the robot.
     *
     * - [Idle]: no person is currently detected.
     * - [Lost]: a previously detected person is no longer detected.
     * - [Detected]: a person is currently detected in front of the robot.
     */
    enum class DetectionState { Idle, Lost, Detected }

    /**
     * The person-detection state changed. Can be used to trigger greetings.
     *
     * @property status The new detection state.
     */
    data class DetectionStatusChanged(val status: DetectionState) : RobotEvent

    /**
     * A single point on the map, used as one waypoint of a path.
     *
     * This is a helper type for [PathChanged] and not an event itself.
     *
     * @property x X coordinate in map coordinates.
     * @property y Y coordinate in map coordinates.
     */
    data class MapPoint(val x: Float, val y: Float)

    /**
     * The route the robot plans to drive has changed.
     *
     * @property path Ordered waypoints from the robot's position to the target.
     * Can be drawn as the route on the map.
     */
    data class PathChanged(val path: List<MapPoint>) : RobotEvent
}