package com.example.teminavigator.ui.map

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.ui.theme.locationBackground
import com.example.teminavigator.ui.theme.locationForeground
import com.example.teminavigator.ui.theme.pinColor
import com.example.teminavigator.ui.theme.selectedPinColor

/**
 * Interactive floor map (pinch-zoom, pan, zoom buttons, pins, robot, optional route).
 *
 * @param destinations     pins to draw (Home: all, Navigation: listOf(target))
 * @param selectedId       id of the highlighted pin
 * @param robotPose        lambda so the map redraws on pose changes without recomposing the caller
 * @param onSelectDestination null = pins are not tappable (Navigation screen)
 * @param route            polyline in image pixel coordinates, drawn below the pins
 * @param overlay          screen-specific buttons, e.g. settings / accept (use Modifier.align inside)
 */
@Composable
fun InteractiveMap(
    destinations: List<Destination>,
    selectedId: String?,
    robotPose: () -> RobotPose?,
    modifier: Modifier = Modifier,
    onSelectDestination: ((Destination) -> Unit)? = null,
    assetFileName: String = "uni_map.png", // image from assets
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val robotPinBackground = colorScheme.locationBackground // colors to use in canvas
    val robotPinForeground = colorScheme.locationForeground
    val pinColor = colorScheme.pinColor
    val selectedPinColor = colorScheme.selectedPinColor
    val routeColor = colorScheme.primary

    val imageBitmap = remember(assetFileName) {
        context.assets.open(assetFileName).use { stream ->
            BitmapFactory.decodeStream(stream).asImageBitmap()
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    var boxSize by remember { mutableStateOf(Size.Zero) }

    // always call the latest callback from inside pointerInput
    val currentOnTap by rememberUpdatedState(onSelectDestination)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { boxSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .background(colorScheme.tertiary)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    scale = newScale
                    offset = if (newScale == minScale) {
                        Offset.Zero
                    } else {
                        offset + pan
                    }
                }
            }
            .then(
                // tap handling only exists if the screen needs it
                if (onSelectDestination != null) {
                    Modifier.pointerInput(destinations) {
                        detectTapGestures { tap -> // tap on destination pin
                            val hitRadius = 32.dp.toPx() // tap area
                            val pinR = 14.dp.toPx()
                            val hit = destinations
                                .mapNotNull { destination ->
                                    val p = destination.imagePx ?: return@mapNotNull null
                                    // pin coordinates
                                    val tip = imageToScreen(
                                        p, boxSize,
                                        imageBitmap.width,
                                        imageBitmap.height,
                                        scale,
                                        offset
                                    )
                                    val body = Offset(tip.x, tip.y - pinR * 1.5f)
                                    destination to (body - tap).getDistance()
                                }
                                .filter { it.second <= hitRadius } // filter out taps if too far away
                                .minByOrNull { it.second }
                                ?.first
                            if (hit != null) currentOnTap?.invoke(hit)
                        }
                    }
                } else Modifier
            )
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Floor map",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {                       // TODO: check -> lambda should not recompose while panning
                    scaleX = scale; scaleY = scale
                    translationX = offset.x; translationY = offset.y
                }
        )

        Canvas(Modifier.fillMaxSize()) {
            val pinR = 14.dp.toPx()
            val width = imageBitmap.width
            val height = imageBitmap.height
            // internal redeclaration to pass less params (since most stay the same)
            fun toScreen(p: Offset) = imageToScreen(
                p, size, width, height, scale, offset
            )

            // first draw unselected dest's, then selected
            destinations.filter { it.id != selectedId }.forEach { destination ->
                destination.imagePx?.let {
                    drawPin(toScreen(it), pinColor, pinR)
                }
            }
            destinations.find { it.id == selectedId }?.let { selDest ->
                val tip = selDest.imagePx?.let(::toScreen) ?: return@let
                drawPin(tip, selectedPinColor, pinR * 1.5f)
            }
            robotPose()?.let {
                drawRobot(
                    center = toScreen(Offset(it.xPx, it.yPx)),
                    yawDeg = it.yawDeg,
                    radius = pinR,
                    pinBackground = robotPinBackground,
                    pinForeground = robotPinForeground
                )
            }
        }

        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale + 0.3f).coerceIn(minScale, maxScale) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }
            FloatingActionButton(
                onClick = {
                    scale = (scale - 0.3f).coerceIn(minScale, maxScale)
                    if (scale == minScale) offset = Offset.Zero
                }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }
        }

        // screen-specific buttons (e.g. settings, accept)
        overlay()
    }
}