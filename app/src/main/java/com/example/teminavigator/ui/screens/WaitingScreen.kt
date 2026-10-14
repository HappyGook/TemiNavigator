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

        // Element 1
        Icon(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 2
        Text(
            strings.greeting,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Element 3
        Text(
            strings.locationQuery,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
            fontSize = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 4
        /**
         * Navigating buttons
         */
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Button to go to the current destination screen
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

        // Element 5
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


@Preview(showBackground = true)
@Composable
fun WaitingScreenPreview() {
    MaterialTheme {
        CompositionLocalProvider(LocalStrings provides germanStrings) {
        WaitingScreen({ }, returnCountdown = null)
        }
    }
}
