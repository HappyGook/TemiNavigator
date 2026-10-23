package com.example.teminavigator.domain
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class MapCalibration(
    val originPx: Offset,
    val pxPerMeter: Float,
    val anchorYaw: Float,
) {
    private val alpha = (PI / 2 - anchorYaw).toFloat()
    private val cosine = cos(alpha)
    private val sine = sin(alpha)

    fun robotToImagePx(
        x: Float,
        y: Float,
    ): Offset {
        val xi = cosine * x - sine * y
        val yi = sine * x + cosine * y
        val offset =
            Offset(
                originPx.x + pxPerMeter * xi,
                originPx.y - pxPerMeter * yi,
            )
        return offset
    }

    fun arrowRotationDeg(yaw: Float): Float = Math.toDegrees((anchorYaw - yaw).toDouble()).toFloat()
}

data class RobotPose(
    val xPx: Float,
    val yPx: Float,
    val yawDeg: Float,
)
