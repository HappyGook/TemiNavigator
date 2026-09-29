package com.example.teminavigator.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.LocalStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit,
){
    val strings = LocalStrings.current

    Scaffold{innerPadding ->
        Column(Modifier.padding(16.dp)){
            Text(strings.settingsTitle, Modifier.padding(innerPadding))
            FloatingActionButton(onClick = onBack, Modifier.padding(innerPadding)) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Go Back")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.languageLabel)
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = (language == currentLanguage),
                        onClick = { onLanguageChanged(language) },
                        label = { Text(language.nativeName) }
                    )
                }
            }
            SettingsColumn()
        }
    }
}

data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val enabled: Boolean = false
)

/**
 * Function for the whole column of settings that have a switch to turn on/off
 * todo: add states to viewmodel so settings are saved
 */
@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    val soundsEnabled = rememberSaveable { mutableStateOf(true) }
    val voiceOutputEnabled = rememberSaveable { mutableStateOf(true) }
    val voiceInputEnabled = rememberSaveable { mutableStateOf(true) }
    val autoReturnEnabled = rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sound setting
        SettingRow(
            icon = Icons.Default.SurroundSound,
            title = strings.sounds,
            description = strings.descriptionSounds,
            checked = soundsEnabled.value,
            onCheckedChange = { soundsEnabled.value = it }
        )

        // Voice Output setting
        SettingRow(
            icon = Icons.Default.Speaker,
            title = strings.voiceOutput,
            description = strings.descriptionVoiceOutput,
            checked = voiceOutputEnabled.value,
            onCheckedChange = { voiceOutputEnabled.value = it }
        )

        // Voice input setting
        SettingRow(
            icon = Icons.Default.Mic,
            title = strings.voiceInput,
            description = strings.descriptionVoiceInput,
            checked = voiceInputEnabled.value,
            onCheckedChange = { voiceInputEnabled.value = it }
        )

        // Auto return to home location setting
        SettingRow(
            icon = Icons.Default.Home,
            title = strings.autoReturn,
            description = strings.descriptionAutoReturn,
            checked = autoReturnEnabled.value,
            onCheckedChange = { autoReturnEnabled.value = it }
        )
    }
}

/**
 * Function for creating a setting row
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

