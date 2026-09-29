package com.example.teminavigator.ui.screens

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
import androidx.compose.ui.unit.sp
import com.example.teminavigator.R
import com.example.teminavigator.ui.langs.LocalStrings

@Composable
fun WaitingScreen() {
    val strings = LocalStrings.current

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
                painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier.height(60.dp).width(220.dp),
                onClick = { /* todo: go to navigation screen */ },
            )

            // Button to go to the current destination screen
            AppButton(
                strings.backToNavigation,
                painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier.height(60.dp).width(220.dp),
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