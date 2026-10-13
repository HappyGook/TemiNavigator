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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.RobotPose
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.langs.germanStrings
import com.example.teminavigator.ui.map.InteractiveMap
import com.example.teminavigator.ui.theme.TemiNavigatorTheme

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
                            textStyle = TextStyle(fontSize = 28.sp),
                            placeholder = { Text(strings.searchDestinations, fontSize = 28.sp) }
                        )

                        // Microphone button -> starts voice input
                        IconButton(
                            onClick = {
                                onStartSpeechRecognition()
                            }
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = strings.voiceInput, Modifier.size(72.dp))
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
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(56.dp)
                    ) { Icon(Icons.Default.Settings, contentDescription = "Settings", Modifier.size(40.dp)) }

                    FloatingActionButton(
                        onClick = { showConfirmationDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .height(72.dp)
                            .widthIn(min=240.dp)
                    ) { Text(strings.acceptButton, fontSize = 28.sp) }
                }
            }
        }
    }
    if(showConfirmationDialog && selected!=null){
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(strings.confirmTitle(selected.displayName), fontSize = 36.sp) },
            confirmButton ={
                TextButton(
                    modifier = Modifier.height(72.dp),
                    onClick = {
                        showConfirmationDialog = false
                        onDestinationConfirmed(selected) // to ViewModel
                    }
                ) { Text(strings.confirmYes, fontSize = 28.sp)}
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.height(72.dp),
                    onClick = {
                        showConfirmationDialog = false
                    }
                ) { Text(strings.confirmNo, fontSize = 28.sp)}
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
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            LazyRow {
                items(location.aliases){
                        alias ->
                    Text(
                        text = "$alias ",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 24.sp
                        )}
            }
        }
    }
}



@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
fun HSPreview(){
    val mockDest = Destination(
        id = "mock stuff",
        displayName = "Cool Name",
        aliases = emptyList(),
        imagePx = Offset(1167f, 1167f),
        imageYawDeg = 1.6f
    )
    TemiNavigatorTheme {
        CompositionLocalProvider(LocalStrings provides germanStrings) {
        HomeScreen(
            listOf(mockDest),
            robotPose = { RobotPose(0f,0f,0f) },
            onDestinationConfirmed = {},
            onOpenSettings = { },
            onStartSpeechRecognition = { }
        )
        }
    }
}
