package com.example.teminavigator.ui.screens
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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

/**
 * This is the main screen of the app, where the users can select and confirm their destination.
 *
 * The screen is divided into two panels:
 *   - Left Panel: Contains the searchable list of destinations to choose from
 *   - Right Panel: Contains an interactive map ([InteractiveMap]), which shows all destinations,
 *     the currently planned [path] if a destination is chosen and the live robot position.
 *     There is also setting button in the top-right corner to get into the [SettingsScreen]
 *
 * @param destinations              List of all known destinations shown on the map
 * @param selectedId                ID of the currently selected destination
 * @param onSelect                  This is called when the user pick a destination from map or list
 * @param onDeselect                Called when user navigates back from detail view
 * @param path                      List of coordinates for the planned route
 * @param robotPose                 The current position and orientation of the robot on the map
 * @param onDestinationConfirmed    Called when the user confirms navigation in the detail view
 *                                  Triggers the actual navigation of the Temi robot.
 * @param onOpenSettings            Called when the settings button in the map panel is tapped
 * @param onStartSpeechRecognition  Called when the microphone button is tapped to start
 *                                  speech recognition on the Temi-Robot
 * @param voiceInputEnabled         Whether voice input is currently available
 * @param map                       The map image, or `null` if no map is loaded
 */
@Composable
fun HomeScreen(
    destinations: List<Destination>,
    selectedId: String?,
    onSelect: (Destination) -> Unit,
    onDeselect: () -> Unit,
    path: List<Offset>?,
    robotPose: () -> RobotPose?,
    onDestinationConfirmed: (Destination) -> Unit,
    onOpenSettings: () -> Unit,
    onStartSpeechRecognition: () -> Unit,
    voiceInputEnabled: Boolean,
    map: ImageBitmap?,
) {
    // Current search query entered by the user
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selected = destinations.find { it.id == selectedId }
    val strings = LocalStrings.current

    // List of destinations - filtered by the current search query
    // Destination matches if it's display name, id or any aliases contain the search text
    val filteredDestinations =
        destinations.filter { destination ->
            searchQuery.isBlank() ||
                destination.displayName.contains(searchQuery, ignoreCase = true) ||
                destination.id.contains(searchQuery, ignoreCase = true) ||
                destination.aliases.any { it.contains(searchQuery, ignoreCase = true) }
        }

    Scaffold { innerPadding ->
        Row(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(12.dp)
                    .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ---- Left panel: destinations list ----
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainer,
                modifier =
                    Modifier
                        .fillMaxWidth(0.3f)
                        .fillMaxHeight(),
            ) {
                if (selected != null) {
                    DestinationDetail(
                        destination = selected,
                        onBack = { onDeselect() },
                        onConfirm = { onDestinationConfirmed(selected) },
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 28.sp),
                                placeholder = { Text(strings.searchDestinations, fontSize = 28.sp) },
                            )
                            // Microphone button -> starts voice input
                            IconButton(
                                enabled = voiceInputEnabled,
                                onClick = {
                                    onStartSpeechRecognition()
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = strings.voiceInput,
                                    Modifier.size(72.dp),
                                )
                            }
                        }
                        // Filtered list of destinations (filtered by search query)
                        LocationsList(
                            filteredDestinations,
                            selectedId = selectedId,
                            onSelect = { onSelect(it) },
                        )
                    }
                }
            }

            // ---- Right panel: interactive map ----
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.tertiary,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
            ) {
                // Interactive map showing destinations, planned path, live robot pose.
                InteractiveMap(
                    destinations = destinations,
                    selectedId = selectedId,
                    onSelectDestination = { onSelect(it) },
                    path = path,
                    robotPose = robotPose,
                    modifier = Modifier.padding(8.dp),
                    imageBitmap = map,
                ) {
                    FloatingActionButton(
                        onClick = onOpenSettings,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(96.dp),
                    ) { Icon(Icons.Default.Settings, contentDescription = "Settings", Modifier.size(72.dp)) }
                }
            }
        }
    }
}

/**
 * Scrollable list of destinations.
 *
 * Automatically scrolls to the currently selected destination whenever the
 * selection or the list content changes, so that the selection stays visible
 * (e.g. when a destination is picked via voice input or by tapping the map).
 *
 * @param locationList      The destinations to display (already filtered by the caller)
 * @param selectedId        ID of the currently selected destination, used for highlighting
 *                          and auto-scrolling; `null` if nothing is selected
 * @param onSelect          Called when the user taps a list entry
 */
@Composable
fun LocationsList(
    locationList: List<Destination>,
    selectedId: String?,
    onSelect: (Destination) -> Unit,
) {
    val listState = rememberLazyListState()

    // Scroll to the selected destination whenever the selection or list changes
    LaunchedEffect(selectedId, locationList) {
        val selectedIndex = locationList.indexOfFirst { it.id == selectedId }
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    if (locationList.isEmpty()) {
        // Placeholder shown when the list has no entries
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = LocalStrings.current.unreachableInList,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                color = Color.DarkGray,
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(vertical = 5.dp),
        ) {
            items(locationList, key = { it.id }) { location ->
                PossibleLocation(
                    location = location,
                    isSelected = location.id == selectedId,
                    onClick = onSelect,
                )
            }
        }
    }
}

/**
 * Detailed View which is shown when a selection is selected
 *
 * Replaces the destination list when a destination is selected and shows destination name,
 * aliases and a confirmation button to start the navigation.
 * Also has a return button to get back to the destination list view.
 *
 * @param destination   The selected destination to display
 * @param onBack        Called when user taps return button, gets back to the destination list
 * @param onConfirm     Called when confirm button is selected, starts navigation to destination
 */
@Composable
fun DestinationDetail(
    destination: Destination,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Back to the list
        TextButton(onClick = onBack, modifier = Modifier.height(72.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(strings.selectLocation, fontSize = 36.sp)
        }

        // Destination name the user is about to confirm.
        Text(
            text = strings.confirmTitle(destination.displayName),
            fontSize = 48.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
        )
        // Alternative names
        if (destination.aliases.isNotEmpty()) {
            Text(
                text = destination.aliases.joinToString(", "),
                fontSize = 24.sp,
                color = colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        // Confirm button -> starts navigation to this destination
        Button(
            onClick = onConfirm,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(88.dp),
        ) { Text(strings.confirmYes, fontSize = 32.sp) }
    }
}

/**
 * An entry in the destinations list [LocationsList]
 *
 * Displays name and aliases of the destination.
 * A selected entry is highlighted by using another color than the unselected entries.
 *
 * @param location      Location of the destination represented by this row
 * @param isSelected    Boolean to determine if entry is selected
 * @param onClick       Called when the row with the destination is tapped
 */
@SuppressLint("RememberInComposition")
@Composable
fun PossibleLocation(
    location: Destination,
    isSelected: Boolean,
    onClick: (Destination) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = colorScheme.tertiary,
                shape = RoundedCornerShape(12.dp),
            ).background(
                if (isSelected) {
                    colorScheme.primaryContainer
                } else {
                    colorScheme.surface
                },
            ).hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = { onClick(location) },
            ).padding(16.dp),
    ) {
        Column {
            Text(
                text = location.displayName,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )
            LazyRow {
                items(location.aliases) { alias ->
                    Text(
                        text = "$alias ",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 24.sp,
                    )
                }
            }
        }
    }
}

/**
 * Preview of [HomeScreen] for Android Studio's preview pane.
 */
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
fun HSPreview() {
    val mockDest =
        Destination(
            id = "mock stuff",
            displayName = "Cool Name",
            aliases = emptyList(),
            imagePx = Offset(1167f, 1167f),
            imageYawDeg = 1.6f,
        )
    TemiNavigatorTheme {
        CompositionLocalProvider(LocalStrings provides germanStrings) {
            HomeScreen(
                listOf(mockDest),
                robotPose = { RobotPose(481f, 1181f, 0f) },
                onDestinationConfirmed = {},
                onOpenSettings = { },
                onStartSpeechRecognition = { },
                voiceInputEnabled = true,
                selectedId = "mock stuff",
                onSelect = {},
                onDeselect = {},
                path = listOf(Offset(1150f, 1100f), Offset(1050f, 1000f), Offset(1000f, 1100f)),
                map = null,
            )
        }
    }
}
