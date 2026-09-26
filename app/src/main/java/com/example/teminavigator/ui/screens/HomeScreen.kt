package com.example.teminavigator.ui.screens
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.ui.theme.TemiNavigatorTheme

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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Temi Navigator")
                }
            ) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row{
                Surface(
                    modifier = Modifier.fillMaxWidth(0.3f).fillMaxHeight()
                ){
                    LocationsList(mockDestinations)
                }
                Surface(
                    color = Color.Blue,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                ){
                    InteractiveMap()
                }
            }

        }
    }
}

@Composable
fun LocationsList(locationList: List<Destination>){
        LazyColumn() {
            items(locationList){location -> PossibleLocation(location=location)}
        }
}

@Composable
fun PossibleLocation(location : Destination){
    Row(Modifier.fillMaxWidth(0.9f)){
        Column{
            Text(location.displayName)
            LazyRow {
                items(location.aliases){alias -> Text("$alias ")}
            }
        }
    }
}

@Composable
fun InteractiveMap(
    modifier: Modifier = Modifier,
    assetFileName: String = "uni_map.png" // image from assets
) {
    val context = LocalContext.current

    val imageBitmap = remember {
        context.assets.open(assetFileName).use { stream ->
            BitmapFactory.decodeStream(stream).asImageBitmap()
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.tertiary)
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
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Floor map",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // TODO: add pins on the map

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
    }
}

@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
fun HSPreview(){
    TemiNavigatorTheme {
        HomeScreen()
    }
}