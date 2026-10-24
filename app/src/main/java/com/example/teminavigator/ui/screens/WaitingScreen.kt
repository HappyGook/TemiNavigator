package com.example.teminavigator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teminavigator.ui.langs.LocalStrings
import com.example.teminavigator.ui.langs.germanStrings

/**
 * This is an idle screen which is shown after the robot successfully finished a guidance and waits
 * for either user input or to return to their home.
 *
 * This screen displays a greeting for the user. It has a single button to leave the waiting state
 * and go into the HomeScreen to select a destination.
 *
 * While idle, the [returnCountdown] is shown to display the time until the robot navigates back
 * to their home base (if set in [SettingsScreen]).
 *
 *
 * @param onLeaveWaiting    Called when user taps "select location" button,
 *                          navigates to the destination selection [HomeScreen]
 * @param returnCountdown   Seconds until the robot automatically returns to its home location
 *                          or null if no countdown is currently running
 */
@Composable
fun WaitingScreen(
    onLeaveWaiting: () -> Unit,
    returnCountdown: Int?
) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize().
        systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Element 1 - waiting icon
        Icon(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 2: greeting headline
        Text(
            strings.greeting,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Element 3: prompt asking the user for their destination
        Text(
            strings.locationQuery,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
            fontSize = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 4
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Button to go to the HomeScreen
            AppButton(
                strings.selectLocation,
                Icons.Filled.LocationOn,
                modifier = Modifier.height(90.dp).width(380.dp),
                onClick = onLeaveWaiting,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Element 4b - Returning home message
        Box(
            modifier = Modifier.height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            returnCountdown?.let { seconds ->
                Text(
                    strings.returningHomeIn(seconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 36.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Element 5: voice input hint (mic icon + notice text)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .alignByBaseline() // shift up a bit so it sits visually centered with lowercase text height
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                strings.speechNotice,
                fontSize = 36.sp,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
    }
}

/**
 * Function for a reusable large button with an icon and a label
 *
 * @param text      Button label
 * @param icon      Icon shown before the label
 * @param onClick   Called when the button is pressed
 * @param modifier  Layout modifier (e.g. size) applied to the button
 * @param colors    Button color scheme; defaults to the standard filled button colors
 */
@Composable
fun AppButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(onClick = onClick, modifier = modifier, colors = colors) {

        /* Inside button body */
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 36.sp, lineHeight = 36.sp)
    }
}

/**
 * Preview of [WaitingScreen] for Android Studio's preview pane.
 */
@Preview(showBackground = true)
@Composable
fun WaitingScreenPreview() {
    MaterialTheme {
        CompositionLocalProvider(LocalStrings provides germanStrings) {
        WaitingScreen({ }, returnCountdown = null)
        }
    }
}
