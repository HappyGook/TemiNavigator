package com.example.teminavigator.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.NavigationState
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.langs.germanStrings
import com.example.teminavigator.ui.map.InteractiveMap
import com.example.teminavigator.ui.theme.TemiNavigatorTheme
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun NavigationScreen(
    destination: Destination,
    robotPose: () -> RobotPose?,
    currentSpeed: () -> Float,
    pxPerMeter: Float?,
    navigationState: NavigationState,
    onTogglePause: () -> Unit,
    onAbort: () -> Unit,
    path: List<Offset>?,
    map: ImageBitmap?
) {
    val strings = LocalStrings.current
    val paused = navigationState is NavigationState.Paused
    var startPose by remember(destination) { mutableStateOf<RobotPose?>(null) }
    val arrived = navigationState as? NavigationState.Arrived

    // take a start pose once on launch of the screen for further calculations
    LaunchedEffect(destination) {
        startPose = snapshotFlow { robotPose() }.filterNotNull().first()
    }

    // meter distance calculated from current pose and destination
    val distanceM: Float? by remember(destination, robotPose, pxPerMeter) {
        derivedStateOf {
            val ppm = pxPerMeter?.takeIf { it > 0f } ?: return@derivedStateOf null
            val pose = robotPose() ?: return@derivedStateOf null
            val target = destination.imagePx ?: return@derivedStateOf null
            hypot(target.x - pose.xPx, target.y - pose.yPx) / ppm
        }
    }

    // current speed passed from viewmodel
    val speed = currentSpeed()
    val d = distanceM

    // estimated time of arrival
    val etaMin: Int? =
        if (d != null && speed > 0f) ceil(d / speed / 60f).toInt().coerceAtLeast(1)
        else null

    // progress - how much of the overall distance has already been covered
    val progress by remember(destination) {
        derivedStateOf {
            val start = startPose
            val pose = robotPose()
            val target = destination.imagePx
            if (start == null || pose == null || target == null) 0f
            else {
                val total = hypot(target.x - start.xPx, target.y - start.yPx)
                val remaining = hypot(target.x - pose.xPx, target.y - pose.yPx)
                if (total > 0f) (1f - remaining / total).coerceIn(0f, 1f) else 0f
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTogglePause)
    ) {

        /** Left side of the screen */
        Column(
            modifier = Modifier
                .weight(1f)     // note: weight splits the row, for now, right side  gets 1/3, left 2/3
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Element 1
            Text(strings.goingTo, style = MaterialTheme.typography.labelMedium, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Element 2 - Destination String
            Text(
                destination.displayName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Element 3
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = etaMin?.let { "Ca. $it ${strings.minutes}" } ?: "– ${strings.minutes}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                    color = Color.DarkGray
                )

                Text(
                    text = distanceM?.let { " -  ${it.roundToInt()} m" } ?: " -  – m",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                    color = Color.DarkGray
                )

            }
            Spacer(modifier = Modifier.height(16.dp))

            // Element 4 - Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Element 5 - Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Pause Navigation
                AppButton(
                    text = if (navigationState is NavigationState.Paused) {
                        strings.resumeNavigation
                    } else {
                        strings.pauseNavigation
                    },
                    icon = if (navigationState is NavigationState.Paused) {
                        Icons.Filled.PlayArrow
                    } else {
                        Icons.Filled.Pause
                    },
                    onClick = onTogglePause,
                    Modifier.height(144.dp)
                )

                if(paused){
                    // Abort Navigation
                    AppButton(
                        strings.abortNavigation,
                        Icons.Filled.StopCircle,
                        onClick =  onAbort,
                        Modifier.height(144.dp)
                    )
                }
            }
        }

        /** Right side - Map */
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
        ) {
            InteractiveMap(
                destinations = listOf(destination),
                selectedId = destination.id,
                robotPose = robotPose,
                path = path,
                imageBitmap = map
            )
        }
    }
    if (arrived != null && arrived.target == destination) {
        ArrivalOverlay(name = destination.displayName)
    }
}

@Composable
private fun ArrivalOverlay(name: String) {
    val strings = LocalStrings.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(96.dp))
                Text(strings.spokenArrived(name), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text(name, style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}

@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
fun NavigationScreenPreview() {
    val mockDest = Destination(
        id = "mock stuff",
        displayName = "Cool Name",
        aliases = emptyList(),
        imagePx = Offset(1167f, 1167f),
        imageYawDeg = 1.6f
    )
    TemiNavigatorTheme {
        CompositionLocalProvider(LocalStrings provides germanStrings) {
            NavigationScreen(
                destination = mockDest,
                robotPose = { RobotPose(1030f, 530f, 0.5f) },
                currentSpeed = { 0.5f },
                pxPerMeter = 42f,
                navigationState = NavigationState.Paused(mockDest),
                onTogglePause = {},
                onAbort = {},
                path = listOf(Offset(1150f,1100f), Offset(1050f,1000f), Offset(1000f,1100f)),
                map = null
            )
        }
    }
}