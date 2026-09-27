package com.example.robui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.robui.R

@Composable
fun WaitingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Element 1
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Message icon",
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 2
        Text(
            "Hallo, schön dich zu sehen!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 3
        Text(
            "Wo darf ich dich hinbringen?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Element 4
        /**
         * Navigating buttons
         */
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Button to go to navigation screen
            AppButton(
                "Ort auswählen",
                painterResource(R.drawable.ic_launcher_foreground),
                onClick = { /* todo: go to navigation screen */ },
            )

            // Button to go to the current destination screen
            AppButton(
                "Zurück zur Navigation",
                painterResource(R.drawable.ic_launcher_foreground),
                onClick = { /* todo: go to navigation screen */ }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Element 5
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Message icon",
                modifier = Modifier
                    .size(20.dp)
                    .alignByBaseline() // shift up a bit so it sits visually centered with lowercase text height
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                "Du kannst auch sagen: \"Bring mich zu Raum 2.72\"",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AppButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(onClick = onClick, modifier = modifier, colors = colors) {

        /* Inside button body */
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}


@Preview(showBackground = true)
@Composable
fun WaitingScreenPreview() {
    MaterialTheme {
        WaitingScreen()
    }
}

