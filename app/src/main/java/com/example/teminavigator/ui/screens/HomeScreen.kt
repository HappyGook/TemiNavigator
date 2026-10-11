package com.example.teminavigator.ui.screens
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.teminavigator.ui.map.InteractiveMap

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
    onOpenSettings: () -> Unit,
    onStartSpeechRecognition: () -> Unit
) {
    var selectedId by rememberSaveable{mutableStateOf<String?>(null)}
    var showConfirmationDialog by rememberSaveable {mutableStateOf(false)}
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selected = destinations.find{it.id==selectedId}
    val strings = LocalStrings.current

    // List of destinations - filtered by the current search query
    val filteredDestinations = destinations.filter { destination ->
        searchQuery.isBlank() ||
            destination.displayName.contains(searchQuery, ignoreCase = true) ||
            destination.id.contains(searchQuery, ignoreCase = true) ||
            destination.aliases.any { it.contains(searchQuery, ignoreCase = true) }
    }

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
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search input -> bound to searchQuery state
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it        // update search text
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text(strings.searchDestinations) }
                        )

                        // Microphone button -> starts voice input
                        IconButton(
                            onClick = {
                                onStartSpeechRecognition()
                            }
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = strings.voiceInput)
                        }
                    }
                    // Filtered list of destinations (filtered by search query)
                    LocationsList(
                        filteredDestinations,
                        selectedId = selectedId,
                        onSelect = { selectedId = it.id }
                    )
                }
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
                    modifier = Modifier.padding(8.dp)
                ){
                    FloatingActionButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) { Icon(Icons.Default.Settings, contentDescription = "Settings") }

                    FloatingActionButton(
                        onClick = { showConfirmationDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    ) { Text(strings.acceptButton) }
                }
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
                isSelected = location.id == selectedId, // TODO: scroll list onSelect to the selected Id
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