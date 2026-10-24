package com.example.teminavigator.domain
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Transforms robot coordinates into pixel coordinates on the map image.
 *
 * The robot reports positions in its own coordinate system (meters). To draw
 * the robot and destinations on the map image, each position is:
 * 1. rotated by `alpha = PI/2 - anchorYaw`, so the robot's axes line up with
 *    the map's,
 * 2. scaled by [pxPerMeter], and
 * 3. shifted to [originPx]. The image's Y-axis points downwards, so the
 *    y component is subtracted.
 *
 * Instances are immutable. The values are persisted by [com.example.teminavigator.data.CalibrationRepository].
 *
 * @property originPx Pixel position on the map image of the robot's origin
 * (the home base).
 * @property pxPerMeter Scale of the map: how many image pixels equal one meter
 * in the real world.
 * @property anchorYaw Robot yaw (radians) at the anchor pose that points to the
 * top of the map image. It defines the rotation between the robot's coordinate
 * system and the map.
 */
data class MapCalibration(
    val originPx: Offset,
    val pxPerMeter: Float,
    val anchorYaw: Float,
) {

    /**
     * Rotation angle (radians) applied to robot coordinates.
     * Precomputed together with [cosine] and [sine] since they never change.
     * */
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

    /**
     * Converts a robot heading into a rotation for the robot arrow on the map.
     *
     * Returns 0° when the robot faces the same way as at the anchor pose
     * ([anchorYaw]).
     *
     * @param yaw Robot heading in radians.
     * @return Rotation for the arrow icon, in degrees.
     */
    fun arrowRotationDeg(yaw: Float): Float = Math.toDegrees((anchorYaw - yaw).toDouble()).toFloat()
}

/**
 * Pose of the robot on the map image, ready to draw.
 *
 * This is the result of converting a robot pose with [MapCalibration].
 *
 * @property xPx X position on the map image, in pixels.
 * @property yPx Y position on the map image, in pixels.
 * @property yawDeg Rotation of the robot arrow, in degrees.
 */
data class RobotPose(
    val xPx: Float,
    val yPx: Float,
    val yawDeg: Float,
)
