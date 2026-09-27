package com.example.robui.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.robui.R

@Composable
fun NavigationScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /** Left side of the screen */
        // Element 1
        Text(
            "UNTERWEGS NACH",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 2
        Text(
            "Ziel", /* todo: put variable for destination name here */
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 3
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Route icon",
                modifier = Modifier
                    .size(20.dp)
                    .alignByBaseline() // shift up a bit so it sits visually centered with lowercase text height
            )

            Text(
                "Noch ca. 2 Minuten", /* todo: put variable for expected duration */
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Text(
                "-",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Text(
                "85 m", /* todo: put variable for remaining distance */
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Element 4 - Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Button to go to navigation screen
            AppButton(
                "Pause",
                painterResource(R.drawable.ic_launcher_foreground),
                onClick = { /* todo: go to navigation screen */ },
            )

            // Button to go to the current destination screen
            AppButton(
                "Fahrt abbrechen",
                painterResource(R.drawable.ic_launcher_foreground),
                onClick = { /* todo: go to navigation screen */ }
            )
        }
    }
}