package com.example.teminavigator.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate


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

fun DrawScope.drawRobot(center: Offset, yawDeg: Float, radius: Float) {
        drawCircle(Color(0x552196F3), radius * 1.8f, center)   // TODO: replace with theme colors
        drawCircle(Color(0xFF2196F3), radius, center)
        drawCircle(Color.White, radius, center, style = Stroke(width = radius * 0.2f))
        // arrow pointing up at 0 degrees, rotated by yaw
        rotate(degrees = yawDeg, pivot = center) {
            val arrow = Path().apply {
                moveTo(center.x, center.y - radius * 2.4f)
                lineTo(center.x - radius * 0.7f, center.y - radius * 1.2f)
                lineTo(center.x + radius * 0.7f, center.y - radius * 1.2f)
                close()
            }
            drawPath(arrow, Color(0xFF2196F3))
        }
    }
