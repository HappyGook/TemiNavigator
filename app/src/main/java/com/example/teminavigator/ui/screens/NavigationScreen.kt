package com.example.teminavigator.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.ui.langs.LocalStrings
import com.google.android.libraries.mapsplatform.transportation.consumer.model.Route

@Composable
fun NavigationScreen(
    destination: Destination,
    onAbort: () -> Unit,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val mapBitmap = remember {
        context.assets.open("uni_map.png").use { stream ->
            BitmapFactory.decodeStream(stream).asImageBitmap()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {

        /** Left side of the screen */
        Column(
            modifier = Modifier
                .weight(1f)     // note: weight splits the row, for now, right side  gets 1/3, left 2/3
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Element 1
            Text(strings.goingTo, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Element 2 - Destination String
            Text(
                destination.displayName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Element 3
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Ca. 2" + strings.minutes,   // todo: add estimated remaining time parameter
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Text(
                    " -  82 m",    // todo: add remaining distance parameter
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

            }
            Spacer(modifier = Modifier.height(16.dp))

            // Element 4 - Progress bar
            LinearProgressIndicator(
                progress = { 0.35f }, // todo: use currentDistance / totalDistance parameter
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Element 5 - Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Pause Navigation
                AppButton(
                    strings.pause,
                    Icons.Filled.Pause,
                    onClick = { /* todo */ })             // todo: add pause drawable

                // Abort Navigation
                AppButton(
                    strings.abortNavigation,
                    Icons.Filled.StopCircle,
                    onClick =  onAbort
                )
            }

            // Element 6 - simulate arrival button
            OutlinedButton(onClick = { /* todo: simulate arrival */ }) {
                /* Inside button body */
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(strings.simulateArrival)
            }
        }

        /** Right side - Map */
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            Image(
                bitmap = mapBitmap,
                contentDescription = "Floor map",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}