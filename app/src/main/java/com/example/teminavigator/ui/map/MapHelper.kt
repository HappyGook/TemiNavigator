package com.example.teminavigator.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Converts a point on the map image into a position on the screen.
 *
 * Reproduces exactly what the map's `Image` does on screen: the bitmap is
 * fitted into the box with `ContentScale.Fit` (leaving empty bars on the
 * sides if the aspect ratios differ), scaled by [zoom] around the center of
 * the box and then shifted by [pan]. Use it to place pins, the route and the
 * robot on the zoomed and panned map, and to map taps back onto pins.
 *
 * @param p Point in image pixels, relative to the top-left of the bitmap.
 * @param box Size of the map area on screen, in pixels.
 * @param imgW Width of the map bitmap, in pixels.
 * @param imgH Height of the map bitmap, in pixels.
 * @param zoom Current zoom factor (1 = whole map fits the box).
 * @param pan Current shift of the map caused by panning, in screen pixels.
 * @return The position of [p] inside the box, in screen pixels.
 */
fun imageToScreen(
        p: Offset,
        box: Size,
        imgW: Int,
        imgH: Int,
        zoom: Float,
        pan: Offset
    ): Offset {
        val fit = minOf(box.width / imgW, box.height / imgH)
        val letterbox = Offset(
            (box.width - imgW * fit) / 2f,
            (box.height - imgH * fit) / 2f
        )
        val center = Offset(box.width / 2f, box.height / 2f)
        val base = Offset(p.x * fit, p.y * fit) + letterbox
        return (base - center) * zoom + center + pan
    }

/**
 * Draws a destination pin: a filled circle with a white dot, on top of a
 * downward-pointing tip.
 *
 * The pin is anchored by its tip, so [tip] is the exact spot on the map the
 * pin marks. The circle sits above it, at a distance of 2.2 times [radius].
 *
 * @param tip Screen position of the pin's pointed end, which marks the location.
 * @param color Fill color of the pin.
 * @param radius Radius of the pin's circle, in pixels. The rest of the pin scales with it.
 */
fun DrawScope.drawPin(tip: Offset, color: Color, radius: Float) {
        val head = Offset(tip.x, tip.y - radius * 2.2f)
        val tail = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(head.x - radius * 0.85f, head.y + radius * 0.5f)
            lineTo(head.x + radius * 0.85f, head.y + radius * 0.5f)
            close()
        }
        drawPath(tail, color)
        drawCircle(color, radius, head)
        drawCircle(Color.White, radius * 0.4f, head)
    }

/**
 * Draws the robot marker: a circle with a halo and an arrow showing where the
 * robot is facing.
 *
 * The marker is anchored by its center, unlike the pins. The arrow sits at the
 * circle's edge and points up at [yawDeg] = 0, then rotates clockwise around
 * the center.
 *
 * @param center Screen position of the robot.
 * @param yawDeg Rotation of the arrow in degrees (see [com.example.teminavigator.domain.MapCalibration.arrowRotationDeg]).
 * @param radius Radius of the inner circle, in pixels. The halo is 1.8 times larger.
 * @param pinBackground Color of the halo around the robot.
 * @param pinForeground Color of the inner circle and the arrow.
 */
fun DrawScope.drawRobot(center: Offset, yawDeg: Float, radius: Float, pinBackground: Color, pinForeground: Color) {
        drawCircle(pinBackground, radius * 1.8f, center)
        drawCircle(pinForeground, radius, center)
        drawCircle(Color.White, radius, center, style = Stroke(width = radius * 0.2f))
        // arrow pointing up at 0 degrees, rotated by yaw
        rotate(degrees = yawDeg, pivot = center) {
            val arrow = Path().apply {
                moveTo(center.x, center.y - radius * 2.4f)
                lineTo(center.x - radius * 0.7f, center.y - radius * 1.2f)
                lineTo(center.x + radius * 0.7f, center.y - radius * 1.2f)
                close()
            }
            drawPath(arrow, pinForeground)
        }
    }

/**
 * Draws the route as a connected line through all [points].
 *
 * The line is drawn twice: first a wider white outline, then the colored line
 * on top, so it stays readable on any part of the map. Corners and ends are
 * rounded. Does nothing if there are fewer than two points.
 *
 * @param points Waypoints in screen pixels, in driving order.
 * @param color Color of the route line.
 * @param width Width of the colored line in pixels. The outline is 1.6 times wider.
 */
fun DrawScope.drawRoute(points: List<Offset>, color: Color, width: Float) {
    if (points.size < 2) return
    val line = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    // white outline first so the route stays readable
    drawPath(
        line, Color.White,
        style = Stroke(width * 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        line, color,
        style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}