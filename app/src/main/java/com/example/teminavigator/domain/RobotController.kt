package com.example.teminavigator.domain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction of the robot, which the ViewModel and UI depend on instead of
 * the Temi SDK directly.
 *
 * Communication runs in two directions:
 * - **Commands** go to the robot through the functions of this interface.
 * - **Results** come back as [RobotEvent]s through [events] and are handled
 *   by the ViewModel, e.g. as [NavigationState] changes.
 *
 * Because the app only knows this interface, the Temi implementation can be
 * replaced, for example by a fake for tests.
 */
interface RobotController {

    /** Names (ids) of all locations saved on the robot that can be used as destinations. */
    val availableLocations: List<String>

    /**
     * Stream of everything the robot reports, as [RobotEvent]s.
     * Collected to receive events as they occur.
     */
    val events: Flow<RobotEvent>

    /** Whether the robot is ready to receive commands. Observe it to react to changes. */
    var ready: StateFlow<Boolean>

    /**
     * Returns the pose of every saved location.
     *
     * @return Map from location id to its pose as a Triple of (x, y, yaw), in the
     * robot's map coordinate system.
     */
    fun locationPoses(): Map<String, Triple<Double, Double, Double>>

    /** Connects the app to the robot, so events are received. Call when the app becomes active. */
    fun attach()

    /** Disconnects the app from the robot and stops event delivery. Call when the app goes inactive. */
    fun detach()

    /**
     * Starts driving to a saved location.
     *
     * Progress is reported through [RobotEvent.GoToStarted], [RobotEvent.GoToFinished]
     * and [RobotEvent.GoToCancelled].
     *
     * @param locationId Id of the target location, one of [availableLocations].
     */
    fun goTo(locationId: String)

    /** Stops the robot's current movement immediately. */
    fun stop()

    /**
     * Makes the robot say a text aloud.
     *
     * @param text The text to speak.
     * @param languageCode Language of the text, e.g. "de-DE" or "en-US".
     */
    fun speak(text: String, languageCode: String)

    /** Interrupts the robot's current speech output. */
    fun stopSpeaking()

    /** Stops listening for speech input. */
    fun stopSpeechRecognition()

    /**
     * Sets the language used for speech recognition.
     *
     * @param languageCode Language code, e.g. "de-DE" or "en-US".
     */
    fun setSpeechLanguage(languageCode: String)

    /** Starts listening for speech input. Results arrive as [RobotEvent.SpeechRecognised]. */
    fun startSpeechRecognition()

    /** Returns the speed currently used for navigation. */
    fun getSpeed(): NavSpeed

    /**
     * Sets the speed used for navigation.
     *
     * @param speed The new navigation speed.
     */
    fun setSpeed(speed: NavSpeed)
}