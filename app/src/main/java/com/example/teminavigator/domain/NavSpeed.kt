package com.example.teminavigator.domain

/**
 * App-wide navigation speed levels.
 *
 * Maps the Temi SDK's own speed enum to a type the app controls. The UI,
 * settings and [RobotController.getSpeed] / [RobotController.setSpeed]
 * all use this type, so the rest of the app doesn't
 * depend on the SDK. The real velocity of each level is defined in
 * [SpeedCalibration].
 */
enum class NavSpeed { SLOW, MEDIUM, HIGH }

/**
 * Real-world velocity of each [NavSpeed] level, in meters per second.
 *
 * The Temi SDK only offers abstract speed levels, so these values are measured
 * or estimated approximations. They are used to calculate the estimated time
 * of arrival (ETA) shown in the UI: `eta = distance / mps(speed)`.
 *
 * @property slowMps Velocity of [NavSpeed.SLOW] in m/s.
 * @property mediumMps Velocity of [NavSpeed.MEDIUM] in m/s.
 * @property highMps Velocity of [NavSpeed.HIGH] in m/s.
 */
data class SpeedCalibration(
    val slowMps: Float = 0.3f,
    val mediumMps: Float = 0.5f,
    val highMps: Float = 0.7f,
) {

    /**
     * Returns the velocity belonging to a speed level.
     *
     * @param speed The speed level to look up.
     * @return Velocity in meters per second.
     */
    fun mps(speed: NavSpeed): Float =
        when (speed) {
            NavSpeed.SLOW -> slowMps
            NavSpeed.MEDIUM -> mediumMps
            NavSpeed.HIGH -> highMps
        }
}
