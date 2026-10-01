package com.example.teminavigator.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
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

@Composable
fun WaitingScreen(
    onSelectLocation: () -> Unit,
    onBackToNavigation: () -> Unit,
) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Element 1
        Icon(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 2
        Text(
            strings.greeting,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Element 3
        Text(
            strings.locationQuery,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 4
        /**
         * Navigating buttons
         */
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Button to go to navigation screen
            AppButton(
                strings.selectLocation,
                Icons.Filled.LocationOn,
                modifier = Modifier.height(60.dp).width(220.dp),
                onClick = onSelectLocation,
            )

            // Button to go to the current destination screen
            AppButton(
                strings.backToNavigation,
                Icons.Filled.Navigation,
                modifier = Modifier.height(60.dp).width(220.dp),
                onClick = onBackToNavigation
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Element 5
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .alignByBaseline() // shift up a bit so it sits visually centered with lowercase text height
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                strings.speechNotice,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
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
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}


@Preview(showBackground = true)
@Composable
fun WaitingScreenPreview() {
    MaterialTheme {
        WaitingScreen({ Log.i("Info", "Home screen opened") }, { Log.i("Info", "Navigation screen opened") })
    }
}