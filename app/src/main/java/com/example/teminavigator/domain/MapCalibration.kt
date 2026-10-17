package com.example.teminavigator.domain
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class MapCalibration (
    val originPx: Offset,
    val pxPerMeter: Float,
    val anchorYaw: Float
){
    private val alpha = (PI/2 - anchorYaw).toFloat()
    private val cosine = cos(alpha)
    private val sine = sin(alpha)

    fun robotToImagePx(x: Float, y: Float): Offset{
        /*
        Log.i("MapCalibration",
            "robot-to-Image called: \n" +
                    "originPx-X = ${originPx.x}, originPx-Y = ${originPx.y}, " +
                    "pxPerMeter=${pxPerMeter}, anchorYaw = $anchorYaw, alpha = ${alpha} ")
         */
        val xi = cosine * x - sine * y
        val yi = sine * x + cosine * y
        val offset = Offset(originPx.x + pxPerMeter*xi,
            originPx.y - pxPerMeter*yi)
        /*
        Log.i("MapCalibration",
            "robot-to-Image called: \n x=${x}, y=${y};\n " +
                    "transformed: x=${offset.x}, y=${offset.y}")
         */
        return offset
    }

    fun imagePxToRobot(p: Offset): Pair<Float,Float>{
        val xi = (p.x - originPx.x) / pxPerMeter
        val yi = -(p.y - originPx.y) / pxPerMeter
        return (cosine * xi + sine * yi) to (-sine * xi + cosine * yi)
    }

    fun arrowRotationDeg(yaw: Float): Float =
        Math.toDegrees((anchorYaw - yaw).toDouble()).toFloat()
}

data class RobotPose(val xPx: Float, val yPx: Float, val yawDeg: Float)