package com.example.teminavigator.domain

enum class NavSpeed { SLOW, MEDIUM, HIGH }

data class SpeedCalibration(
    val slowMps: Float = 0.3f,
    val mediumMps: Float = 0.5f,
    val highMps: Float = 0.7f,
) {
    fun mps(speed: NavSpeed): Float =
        when (speed) {
            NavSpeed.SLOW -> slowMps
            NavSpeed.MEDIUM -> mediumMps
            NavSpeed.HIGH -> highMps
        }
}
