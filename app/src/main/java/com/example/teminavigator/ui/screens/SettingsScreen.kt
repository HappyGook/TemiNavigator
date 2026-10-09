package com.example.teminavigator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.teminavigator.data.AdminPasswordStore
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    calibration: MapCalibration,
    onCalibrationSaved: (MapCalibration) -> Unit,
    adminPasswordStore: AdminPasswordStore
){
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var isUnlocked by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isCheckingPassword by remember { mutableStateOf(false) }

    // Validates entered password against stored credentials
    // Enables settings when correct password is entered
    fun unlockSettings() {

        if (password.isEmpty() || isCheckingPassword) return

        val enteredPassword = password

        scope.launch {
            // Show "checking" state when password is entered
            isCheckingPassword = true

            // moves verifyPassword to a background thread, so app does not freeze
            val valid = try {
                withContext(Dispatchers.IO) {
                    adminPasswordStore.verifyPassword(enteredPassword)
                }
            } finally {
                isCheckingPassword = false // clear "checking" state
            }

            isUnlocked = valid
            passwordError = !valid
            password = ""   // clears field so password is not left on screen
        }
    }

    Scaffold{innerPadding ->
        Column(
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ){
            Text(strings.settingsTitle)
            FloatingActionButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Go Back")
            }
            if (isUnlocked) {
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
                    ChangeAdminPasswordSection(adminPasswordStore) // todo: adjust space or finde better place for section
                }
                SettingsColumn()
                CalibrationSection(
                    calibration = calibration,
                    onSave = onCalibrationSaved
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.adminPasswordTitle, style = MaterialTheme.typography.titleLarge)
                    Text(strings.adminPasswordPrompt)
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text(strings.adminPasswordLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError,
                        enabled = !isCheckingPassword,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { unlockSettings() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError) {
                        Text(strings.incorrectAdminPassword, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        enabled = password.isNotEmpty() && !isCheckingPassword,
                        onClick = { unlockSettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingPassword) {
                            CircularProgressIndicator()
                        } else {
                            Text(strings.unlockSettingsButton)
                        }
                    }
                }
            }
        }
    }
}


// Set a new password
@Composable
private fun ChangeAdminPasswordSection(adminPasswordStore: AdminPasswordStore) {

    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    val isLongEnough = newPassword.length >= 8
    val passwordsMatch = newPassword == confirmPassword

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(strings.changeAdminPasswordTitle, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                statusMessage = null
            },
            label = { Text(strings.newAdminPasswordLabel) },
            visualTransformation = PasswordVisualTransformation(),
            isError = newPassword.isNotEmpty() && !isLongEnough,
            enabled = !isSaving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                statusMessage = null
            },
            label = { Text(strings.confirmAdminPasswordLabel) },
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            enabled = !isSaving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (statusMessage != null) {
            Text(statusMessage!!, color = MaterialTheme.colorScheme.primary)
        } else if (newPassword.isNotEmpty() && !isLongEnough) {
            Text(strings.adminPasswordTooShort, color = MaterialTheme.colorScheme.error)
        } else if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(strings.adminPasswordsDoNotMatch, color = MaterialTheme.colorScheme.error)
        }
        Button(
            enabled = isLongEnough && passwordsMatch && !isSaving,
            onClick = {
                scope.launch {
                    isSaving = true
                    val passwordToSave = newPassword
                    try {
                        withContext(Dispatchers.IO) {
                            adminPasswordStore.changePassword(passwordToSave)
                        }
                    } finally {
                        isSaving = false
                    }
                    newPassword = ""
                    confirmPassword = ""
                    statusMessage = strings.adminPasswordChanged
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator()
            } else {
                Text(strings.saveAdminPasswordButton)
            }
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

/**
 * Function for Creating a MapCalibration block with inputs and confirmation
 */
@Composable
fun CalibrationSection(
    calibration: MapCalibration,
    onSave: (MapCalibration) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val strings = LocalStrings.current

    var originX by rememberSaveable { mutableStateOf("") }
    var originY by rememberSaveable { mutableStateOf("") }
    var pxPerMeter by rememberSaveable { mutableStateOf("") }
    var anchorYaw by rememberSaveable { mutableStateOf("") }

    // keep old value, if invalid text -> null (shown as error)
    fun parse(text: String, fallback: Float): Float? =
        if (text.isBlank()) fallback else text.replace(',', '.').toFloatOrNull()

    val newX = parse(originX, calibration.originPx.x)
    val newY = parse(originY, calibration.originPx.y)
    val newScale = parse(pxPerMeter, calibration.pxPerMeter)?.takeIf { it > 0f }
    val newYaw = parse(anchorYaw, calibration.anchorYaw)

    val allValid = newX != null && newY != null && newScale != null && newYaw != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(strings.calibrationHeader, style = MaterialTheme.typography.titleLarge)

        // Origin (x and y in one row)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = originX,
                onValueChange = { originX = it },
                label = { Text(strings.originXText) },
                placeholder = { Text(calibration.originPx.x.toString()) },
                isError = newX == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = originY,
                onValueChange = { originY = it },
                label = { Text(strings.originYText) },
                placeholder = { Text(calibration.originPx.y.toString()) },
                isError = newY == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = pxPerMeter,
            onValueChange = { pxPerMeter = it },
            label = { Text(strings.pxPerMeterText) },
            placeholder = { Text(calibration.pxPerMeter.toString()) },
            isError = newScale == null,
            supportingText = { if (newScale == null) Text(strings.pxPerMeterWarning) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = anchorYaw,
            onValueChange = { anchorYaw = it },
            label = { Text(strings.anchorYawText) },
            placeholder = { Text(calibration.anchorYaw.toString()) },
            isError = newYaw == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            enabled = allValid,
            onClick = {
                onSave(
                    MapCalibration(
                        originPx = Offset(newX!!, newY!!),
                        pxPerMeter = newScale!!,
                        anchorYaw = newYaw!!
                    )
                )
                // clear fields so that placeholders show new values
                originX = ""; originY = ""; pxPerMeter = ""; anchorYaw = ""
                focusManager.clearFocus()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.saveCalibrationButton)
        }
    }
}
