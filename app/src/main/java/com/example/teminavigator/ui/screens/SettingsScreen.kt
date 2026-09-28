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


@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit
){
    val strings = LocalStrings.current
    Scaffold{innerPadding ->
        Column(Modifier.padding(innerPadding)){
            Text(strings.settingsTitle, Modifier.padding(innerPadding))
            FloatingActionButton(onClick = onBack, Modifier.padding(innerPadding)) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Go Back")
            }
            Column(Modifier.padding(innerPadding)) {
                Text(strings.languageLabel)
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = (language == currentLanguage),
                        onClick = { onLanguageChanged(language) },
                        label = { Text(language.nativeName) }
                    )
                }
            }
        }
    }
}