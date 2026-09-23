package com.example.teminavigator.domain
import kotlinx.coroutines.flow.Flow

/*
* Abstraction, on which ViewModel and UI will depend, so they don't interact with Temi SDK directly
*
* Robot commands go through funcs
* Things robot returns back go into events, then handled (via NavigationState changes, etc.)
 */
interface RobotController {
    val availableLocations: List<String>
    val events: Flow<RobotEvent> // flow allows collecting events as they arise, like a stream of returns
    fun goTo(locationId: String)
    fun stop()
    fun speak(text: String)
    fun goHome()
}