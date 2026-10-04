package com.example.teminavigator.ui.screens
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.map.drawPin
import com.example.teminavigator.ui.map.drawRobot
import com.example.teminavigator.ui.map.imageToScreen
import com.example.teminavigator.ui.theme.locationBackground
import com.example.teminavigator.ui.theme.locationForeground
import com.example.teminavigator.ui.theme.pinColor
import com.example.teminavigator.ui.theme.selectedPinColor

/*
val mockDestinations = listOf(
    Destination(
        id = "lobby",
        displayName = "Lobby",
        aliases = listOf(
            "Lobby",
            "Eingang",
            "Empfang",
            "Rezeption"
        ),
        mapX = 0.12f,
        mapY = 0.84f
    ),
    Destination(
        id = "raum_2_72",
        displayName = "Raum 2.72",
        aliases = listOf(
            "Raum 2.72",
            "Raum zweikommasiebzig",
            "Robotik-Raum",
            "Robotikraum"
        ),
        mapX = 0.42f,
        mapY = 0.28f
    ),
    Destination(
        id = "cafeteria",
        displayName = "Cafeteria",
        aliases = listOf(
            "Cafeteria",
            "Mensa",
            "Kantine",
            "Essensraum"
        ),
        mapX = 0.76f,
        mapY = 0.67f
    ),
    Destination(
        id = "besprechungsraum_a",
        displayName = "Besprechungsraum A",
        aliases = listOf(
            "Besprechungsraum A",
            "Meetingraum A",
            "Konferenzraum A",
            "Raum A"
        ),
        mapX = 0.63f,
        mapY = 0.19f
    ),
    Destination(
        id = "labor_1",
        displayName = "Labor 1",
        aliases = listOf(
            "Labor 1",
            "Labor eins",
            "Forschungslabor",
            "Laborraum"
        ),
        mapX = 0.28f,
        mapY = 0.51f
    )
)
*/

@Composable
fun HomeScreen(
    destinations: List<Destination>,
    robotPose: () -> RobotPose?,
    onDestinationConfirmed: (Destination) -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedId by rememberSaveable{mutableStateOf<String?>(null)}
    var showConfirmationDialog by rememberSaveable {mutableStateOf(false)}
    val selected = destinations.find{it.id==selectedId}
    val strings = LocalStrings.current

    Scaffold{ innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .fillMaxHeight()
            ) {
                LocationsList(
                    destinations,
                    selectedId = selectedId,
                    onSelect = {selectedId = it.id}
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                InteractiveMap(
                    destinations = destinations,
                    selectedId = selectedId,
                    onSelectDestination = { selectedId = it.id },
                    robotPose = robotPose,
                    modifier = Modifier.padding(8.dp),
                    onSettingsClick = onOpenSettings,
                    acceptEnabled = (selected != null), // accept button enabled only when smth selected
                    onAcceptClick = {showConfirmationDialog = true}
                )
            }
        }
    }
    if(showConfirmationDialog && selected!=null){
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(strings.confirmTitle(selected.displayName)) },
            confirmButton ={
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        onDestinationConfirmed(selected) // to ViewModel
                    }
                ) { Text(strings.confirmYes)}
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                    }
                ) { Text(strings.confirmNo)}
            }
        )
    }
}

@Composable
fun LocationsList(
    locationList: List<Destination>,
    selectedId: String?,
    onSelect: (Destination) -> Unit
){
    LazyColumn(Modifier.padding(vertical = 5.dp)) {
        items(locationList, key = { it.id }){location ->
            PossibleLocation(
                location=location,
                isSelected = location.id == selectedId,
                onClick = onSelect
            )
        }
    }
}

@SuppressLint("RememberInComposition")
@Composable
fun PossibleLocation(
    location : Destination,
    isSelected: Boolean,
    onClick: (Destination) -> Unit = {}
){
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = colorScheme.tertiary,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (isSelected) colorScheme.primaryContainer
                else colorScheme.surface
            )
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = { onClick(location) }
            )
            .padding(16.dp)
    ){
        Column{
            Text(
                text=location.displayName,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            LazyRow {
                items(location.aliases){
                        alias ->
                    Text(
                        text = "$alias ",
                        color = colorScheme.onSurfaceVariant
                        )}
            }
        }
    }
}

@Composable
fun InteractiveMap(
    onSettingsClick: () -> Unit,
    onAcceptClick: () -> Unit,
    destinations: List<Destination>,
    selectedId: String?,
    onSelectDestination: (Destination) -> Unit,
    robotPose: () -> RobotPose?,
    acceptEnabled: Boolean,
    modifier: Modifier = Modifier,
    assetFileName: String = "uni_map.png" // image from assets
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val robotPinBackground = colorScheme.locationBackground // colors to use in canvas
    val robotPinForeground = colorScheme.locationForeground
    val pinColor = colorScheme.pinColor
    val selectedPinColor = colorScheme.selectedPinColor

    val imageBitmap = remember {
        context.assets.open(assetFileName).use { stream ->
            BitmapFactory.decodeStream(stream).asImageBitmap()
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    var boxSize by remember {mutableStateOf(Size.Zero)}

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged{boxSize = Size(it.width.toFloat(), it.height.toFloat())}
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
            .pointerInput(destinations){
                detectTapGestures { tap -> //tap on destination pin
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
                            val body = Offset(tip.x, tip.y - pinR*1.5f)
                            destination to (body - tap).getDistance()
                        }
                        .filter {it.second <= hitRadius} // filter out taps if too far away
                        .minByOrNull { it.second }
                        ?.first
                    if (hit!=null) onSelectDestination(hit)
                }
            }
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

        Canvas(Modifier.fillMaxSize()){
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
                    drawPin(toScreen(it),
                    pinColor, pinR)
                }
            }
            destinations.find { it.id == selectedId }?.let { selDest ->
                val tip = selDest.imagePx?.let (::toScreen) ?: return@let
                drawPin(tip, selectedPinColor, pinR*1.5f)
            }
            robotPose()?.let { drawRobot(
                center = toScreen(Offset(it.xPx, it.yPx)),
                yawDeg = it.yawDeg,
                radius = pinR,
                pinBackground = robotPinBackground,
                pinForeground = robotPinForeground
            ) }
        }


        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ){
            FloatingActionButton(
                onClick = onSettingsClick
            ){
                Icon(Icons.Default.Settings, contentDescription = "Settings")
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

        // Accept button to go to the next step
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ){
            FloatingActionButton(
                onClick=onAcceptClick
                // TODO: different color
            ) {
                Text(strings.acceptButton)
            }
        }
    }
}

/* TODO: fix preview ?
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
fun HSPreview(){
    TemiNavigatorTheme {
        HomeScreen(mockDestinations,
            { Log.i("Info", "Destination Confirmed") }, { Log.i("Info", "Settings opened") })
    }
}
*/