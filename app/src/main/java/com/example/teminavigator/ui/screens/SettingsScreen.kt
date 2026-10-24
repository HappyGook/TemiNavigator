package com.example.teminavigator.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teminavigator.data.AdminPasswordStore
import com.example.teminavigator.data.DestinationLabels
import com.example.teminavigator.domain.Destination
import com.example.teminavigator.domain.MapCalibration
import com.example.teminavigator.domain.NavSpeed
import com.example.teminavigator.ui.langs.AppLanguage
import com.example.teminavigator.ui.langs.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This is the settings screen which is used to display and modify settings for the application.
 *
 * This screen is protected by a password. Only the selection of language and the password
 * prompt are visible unless the correct password is entered.
 *
 * When unlocked, all settings are shown:
 * - Language               (Directly in SettingsScreen)
 * - General settings       ([SettingsColumn])
 * - Map calibration        ([CalibrationSection])
 * - Map management         ([MapSection])
 * - Admin password         ([ChangeAdminPasswordSection])
 * - Destination labels     ([DestinationLabelSection])
 *
 * The screen is stateless apart from local UI state (password input, "checking"
 * and "saving" flags, form fields); all persisted settings are passed in and
 * reported back through `on*Changed` callbacks
 *
 * @param currentLanguage                   The currently selected app language
 * @param onLanguageChanged                 Called with the newly selected language
 * @param onBack                            Called when user taps the back button to leave settings
 * @param calibration                       Current map calibration (origin, px/m scale, anchor yaw)
 * @param onCalibrationSaved                Called with the new calibration when the user saves it
 * @param adminPasswordStore                Password store used to verify and change the password
 * @param destinations                      All known navigation destinations
 * @param labels                            Saved names/aliases per destination id, used to prefill
 *                                          the label editors
 * @param onDisplayNameChanged              Called with (destinationId, newDisplayName)
 *                                          when a display name is saved
 * @param onAliasesChanged                  Called with (destinationId, newAliases)
 *                                          when aliases are saved
 * @param onLabelsReset                     Called with a destination id to reset its labels
 * @param returnHomeDelaySec                Seconds to wait before auto-returning home
 * @param onReturnHomeDelayChanged          Called with the new auto-return delay in seconds
 * @param soundsEnabled                     Whether UI sounds are enabled
 * @param onSoundsEnabledChanged            Called with the new sounds setting
 * @param musicEnabled                      Whether background music is enabled
 * @param onMusicEnabledChanged             Called with the new music setting
 * @param voiceOutputEnabled                Whether spoken voice output is enabled
 * @param onVoiceOutputEnabledChanged       Called with the new voice-output setting
 * @param voiceInputEnabled                 Whether voice input (microphone) is enabled
 * @param onVoiceInputEnabledChanged        Called with the new voice-input setting
 * @param autoReturnEnabled                 Whether the robot automatically returns to its home location
 * @param onAutoReturnEnabledChanged        Called with the new auto-return setting
 * @param homeLocationId                    ID of the destination used as the home/waiting position
 * @param homeRadius                        Radius in meters around home that counts as "at home"
 * @param greetingsEnabled                  Whether the robot greets people it encounters
 * @param greetingsCooldownMs               Minimum pause between two greetings, in milliseconds
 * @param arrivalFailureDelaySec            Delay in seconds before a failed arrival is triggered
 * @param onHomeLocationIdChanged           Called with the new home location id
 * @param onHomeRadiusChanged               Called with the new home radius in meters
 * @param onGreetingsEnabled                Called with the new greetings setting
 * @param onGreetingsCooldownChanged        Called with the new greetings cooldown in milliseconds
 * @param onArrivalFailureCooldownChanged   Called with the new arrival-failure delay in seconds
 * @param navigationSpeed                   The currently selected navigation speed preset
 * @param onNavigationSpeedChanged          Called with the newly selected speed preset
 * @param map                               The current map image, or `null` while loading
 * @param onMapReset                        Called when the user confirms resetting the map
 * @param onNewMapPicked                    Called with the content URI of a newly picked map image
 */
@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    calibration: MapCalibration,
    onCalibrationSaved: (MapCalibration) -> Unit,
    adminPasswordStore: AdminPasswordStore,
    destinations: List<Destination>,
    labels: Map<String, DestinationLabels>,
    onDisplayNameChanged: (String, String) -> Unit,
    onAliasesChanged: (String, List<String>) -> Unit,
    onLabelsReset: (String) -> Unit,
    returnHomeDelaySec: Int,
    onReturnHomeDelayChanged: (Int) -> Unit,
    soundsEnabled: Boolean,
    onSoundsEnabledChanged: (Boolean) -> Unit,
    musicEnabled: Boolean,
    onMusicEnabledChanged: (Boolean) -> Unit,
    voiceOutputEnabled: Boolean,
    onVoiceOutputEnabledChanged: (Boolean) -> Unit,
    voiceInputEnabled: Boolean,
    onVoiceInputEnabledChanged: (Boolean) -> Unit,
    autoReturnEnabled: Boolean,
    onAutoReturnEnabledChanged: (Boolean) -> Unit,
    homeLocationId: String,
    homeRadius: Float,
    greetingsEnabled: Boolean,
    greetingsCooldownMs: Int,
    arrivalFailureDelaySec: Int,
    onHomeLocationIdChanged: (String) -> Unit,
    onHomeRadiusChanged: (Float) -> Unit,
    onGreetingsEnabled: (Boolean) -> Unit,
    onGreetingsCooldownChanged: (Int) -> Unit,
    onArrivalFailureCooldownChanged: (Int) -> Unit,
    navigationSpeed: NavSpeed,
    onNavigationSpeedChanged: (NavSpeed) -> Unit,
    map: ImageBitmap?,
    onMapReset: () -> Unit,
    onNewMapPicked: (Uri) -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    // Local UI state for the password prompt
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
            val valid =
                try {
                    withContext(Dispatchers.IO) {
                        adminPasswordStore.verifyPassword(enteredPassword)
                    }
                } finally {
                    isCheckingPassword = false // clear "checking" state
                }

            isUnlocked = valid
            passwordError = !valid
            password = "" // clears field so password is not left on screen
        }
    }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {

            // Title and back button (back to the home screen)
            Text(strings.settingsTitle, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            FloatingActionButton(
                onClick = onBack,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Go Back", Modifier.size(40.dp))
            }

            // Language selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(strings.languageLabel, fontSize = 32.sp)
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = (language == currentLanguage),
                        onClick = { onLanguageChanged(language) },
                        label = { Text(language.nativeName, fontSize = 24.sp) },
                    )
                }
            }

            // Display Depending on unlocked or not
            if (isUnlocked) {
                // ---- Unlocked: full settings area ----
                SettingsColumn(
                    returnHomeDelaySec = returnHomeDelaySec,
                    onReturnHomeDelayChanged = onReturnHomeDelayChanged,
                    soundsEnabled = soundsEnabled,
                    onSoundsEnabledChanged = onSoundsEnabledChanged,
                    musicEnabled = musicEnabled,
                    onMusicEnabledChanged = onMusicEnabledChanged,
                    voiceOutputEnabled = voiceOutputEnabled,
                    onVoiceOutputEnabledChanged = onVoiceOutputEnabledChanged,
                    voiceInputEnabled = voiceInputEnabled,
                    onVoiceInputEnabledChanged = onVoiceInputEnabledChanged,
                    autoReturnEnabled = autoReturnEnabled,
                    onAutoReturnEnabledChanged = onAutoReturnEnabledChanged,
                    homeLocationId = homeLocationId,
                    onHomeLocationIdChanged = onHomeLocationIdChanged,
                    homeRadius = homeRadius,
                    onHomeRadiusChanged = onHomeRadiusChanged,
                    greetingsEnabled = greetingsEnabled,
                    onGreetingsEnabledChanged = onGreetingsEnabled,
                    greetingsCooldownMs = greetingsCooldownMs,
                    onGreetingsCooldownChanged = onGreetingsCooldownChanged,
                    arrivalFailureDelaySec = arrivalFailureDelaySec,
                    onArrivalFailureCooldownChanged = onArrivalFailureCooldownChanged,
                    navigationSpeed = navigationSpeed,
                    onNavigationSpeedChanged = onNavigationSpeedChanged,
                )
                CalibrationSection( // UI block for map calibration part
                    calibration = calibration,
                    onSave = onCalibrationSaved,
                )

                MapSection(map = map, onMapReset = onMapReset, onNewMapPicked = onNewMapPicked)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChangeAdminPasswordSection(adminPasswordStore)
                }
                DestinationLabelSection(
                    destinations = destinations,
                    labels = labels,
                    onDisplayNameSave = onDisplayNameChanged,
                    onAliasesSave = onAliasesChanged,
                    onReset = onLabelsReset,
                )
            } else {
                // ---- Locked: password prompt ----
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.adminPasswordTitle, style = MaterialTheme.typography.titleLarge, fontSize = 32.sp)
                    Text(strings.adminPasswordPrompt, fontSize = 24.sp)

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = { Text(strings.adminPasswordLabel, fontSize = 24.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError,
                        enabled = !isCheckingPassword,
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions = KeyboardActions(onDone = { unlockSettings() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (passwordError) {
                        Text(strings.incorrectAdminPassword, color = MaterialTheme.colorScheme.error, fontSize = 24.sp)
                    }

                    // Unlock button - shows a spinner while the password is verified
                    Button(
                        enabled = password.isNotEmpty() && !isCheckingPassword,
                        onClick = { unlockSettings() },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                    ) {
                        if (isCheckingPassword) {
                            CircularProgressIndicator()
                        } else {
                            Text(strings.unlockSettingsButton, fontSize = 28.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * UI block for setting a new admin password
 *
 * The new password must be at least 8 characters long and entered twice
 * (confirmation field). Live validation shows inline error hints for a too
 * short password or mismatching entries. Saving runs on a background thread
 * and, on success, clears both fields and shows a confirmation message.
 *
 * @param adminPasswordStore Store the new password is persisted to.
 */
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.changeAdminPasswordTitle, style = MaterialTheme.typography.titleLarge, fontSize = 32.sp)
        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                statusMessage = null
            },
            label = { Text(strings.newAdminPasswordLabel, fontSize = 24.sp) },
            visualTransformation = PasswordVisualTransformation(),
            isError = newPassword.isNotEmpty() && !isLongEnough,
            enabled = !isSaving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                statusMessage = null
            },
            label = { Text(strings.confirmAdminPasswordLabel, fontSize = 24.sp) },
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            enabled = !isSaving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        // Status: success message, or the first applicable validation error
        if (statusMessage != null) {
            Text(statusMessage!!, color = MaterialTheme.colorScheme.primary)
        } else if (newPassword.isNotEmpty() && !isLongEnough) {
            Text(strings.adminPasswordTooShort, color = MaterialTheme.colorScheme.error, fontSize = 24.sp)
        } else if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(strings.adminPasswordsDoNotMatch, color = MaterialTheme.colorScheme.error, fontSize = 24.sp)
        }
        Button(
            enabled = isLongEnough && passwordsMatch && !isSaving,
            onClick = {
                scope.launch {
                    isSaving = true
                    val passwordToSave = newPassword
                    try {
                        // Persist on a background thread so the UI does not freeze
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator()
            } else {
                Text(strings.saveAdminPasswordButton, fontSize = 24.sp)
            }
        }
    }
}

/**
 * Function for the whole column of settings that have a switch to turn on/off
 *
 * Groups all toggle- and stepper-based settings in one vertical column:
 * - Enable/disable sounds, music, voice output, voice input
 * - auto-return to home (with its related delay, home location, and tolerance radius)
 * - Enable disable greetings (with cooldown on how long to wait for next greeting)
 * - arrival-failure delay (how long until navigation is canceled when stuck)
 * - navigation speed selector (slow/medium/fast)
 */
@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    returnHomeDelaySec: Int,
    onReturnHomeDelayChanged: (Int) -> Unit,
    soundsEnabled: Boolean,
    onSoundsEnabledChanged: (Boolean) -> Unit,
    musicEnabled: Boolean,
    onMusicEnabledChanged: (Boolean) -> Unit,
    voiceOutputEnabled: Boolean,
    onVoiceOutputEnabledChanged: (Boolean) -> Unit,
    voiceInputEnabled: Boolean,
    onVoiceInputEnabledChanged: (Boolean) -> Unit,
    autoReturnEnabled: Boolean,
    onAutoReturnEnabledChanged: (Boolean) -> Unit,
    homeLocationId: String,
    onHomeLocationIdChanged: (String) -> Unit,
    homeRadius: Float,
    onHomeRadiusChanged: (Float) -> Unit,
    greetingsEnabled: Boolean,
    onGreetingsEnabledChanged: (Boolean) -> Unit,
    greetingsCooldownMs: Int,
    onGreetingsCooldownChanged: (Int) -> Unit,
    arrivalFailureDelaySec: Int,
    onArrivalFailureCooldownChanged: (Int) -> Unit,
    navigationSpeed: NavSpeed,
    onNavigationSpeedChanged: (NavSpeed) -> Unit,
) {
    val strings = LocalStrings.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Sound setting
        SettingRow(
            icon = Icons.Default.SurroundSound,
            title = strings.sounds,
            description = strings.descriptionSounds,
            checked = soundsEnabled,
            onCheckedChange = onSoundsEnabledChanged,
        )

        // Music setting
        SettingRow(
            icon = Icons.Default.MusicNote,
            title = strings.music,
            description = strings.descriptionMusic,
            checked = musicEnabled,
            onCheckedChange = onMusicEnabledChanged,
        )

        // Voice Output setting
        SettingRow(
            icon = Icons.Default.Speaker,
            title = strings.voiceOutput,
            description = strings.descriptionVoiceOutput,
            checked = voiceOutputEnabled,
            onCheckedChange = onVoiceOutputEnabledChanged,
        )

        // Voice input setting
        SettingRow(
            icon = Icons.Default.Mic,
            title = strings.voiceInput,
            description = strings.descriptionVoiceInput,
            checked = voiceInputEnabled,
            onCheckedChange = onVoiceInputEnabledChanged,
        )

        // Auto return to home location setting
        SettingRow(
            icon = Icons.Default.Home,
            title = strings.autoReturn,
            description = strings.descriptionAutoReturn,
            checked = autoReturnEnabled,
            onCheckedChange = onAutoReturnEnabledChanged,
        )

        // Return delay (only active when auto return is on)
        SettingStepperRow(
            label = strings.returnHomeAfter,
            valueText = "$returnHomeDelaySec s",
            enabled = autoReturnEnabled,
            onMinus = { onReturnHomeDelayChanged(returnHomeDelaySec - 1) },
            onPlus = { onReturnHomeDelayChanged(returnHomeDelaySec + 1) },
        )

        // Which Temi location is the waiting position
        HomeLocationRow(
            label = strings.homeLocation,
            selectedId = homeLocationId,
            enabled = autoReturnEnabled,
            onSelected = onHomeLocationIdChanged,
        )

        // "At home" tolerance radius in meters
        SettingStepperRow(
            label = strings.homeRadius,
            valueText = "%.1f m".format(homeRadius),
            enabled = autoReturnEnabled,
            onMinus = { onHomeRadiusChanged(homeRadius - 0.5f) },
            onPlus = { onHomeRadiusChanged(homeRadius + 0.5f) },
        )

        // Greetings on/off
        SettingRow(
            icon = Icons.Default.RecordVoiceOver,
            title = strings.greetingsSetting,
            description = strings.descriptionGreetings,
            checked = greetingsEnabled,
            onCheckedChange = onGreetingsEnabledChanged,
        )

        // Pause between greetings (only active when greetings are on), shown in seconds
        SettingStepperRow(
            label = strings.greetingsCooldown,
            valueText = "${greetingsCooldownMs / 1000} s",
            enabled = greetingsEnabled,
            onMinus = { onGreetingsCooldownChanged(greetingsCooldownMs - 1_000) },
            onPlus = { onGreetingsCooldownChanged(greetingsCooldownMs + 1_000) },
        )

        // Delay before an arrival failure is triggered
        SettingStepperRow(
            label = strings.arrivalFailureDelayText,
            valueText = "$arrivalFailureDelaySec s",
            enabled = true,
            onMinus = { onArrivalFailureCooldownChanged(arrivalFailureDelaySec - 1) },
            onPlus = { onArrivalFailureCooldownChanged(arrivalFailureDelaySec + 1) },
        )

        // Navigation speed
        SpeedRow(
            label = strings.navigationSpeed,
            selected = navigationSpeed,
            labelFor = {
                when (it) {
                    NavSpeed.SLOW -> strings.speedSlow
                    NavSpeed.MEDIUM -> strings.speedMedium
                    NavSpeed.HIGH -> strings.speedHigh
                }
            },
            onSelected = onNavigationSpeedChanged,
        )
    }
}

/**
 * Function for creating a setting row
 *
 * A single on/off setting: icon on the left, title and description in the middle,
 * and a switch on the right
 *
 * @param icon              Icon shown at the start of the row
 * @param title             Name of the setting
 * @param description       Short explanation of setting shown under the title
 * @param checked           Current value of the setting (on / off)
 * @param onCheckedChange   Called with the new value when the switch is toggled
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
            )
        }

        Spacer(Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(40.dp),
        )
    }
}

/**
 * NavSpeed configuration with 3 buttons, sets the speed of the Temi Robot
 *
 * Shows three labels that can be selected, highlights the selected label
 * Possible presets: slow, medium, high
 *
 * @param label         Name of the setting
 * @param selected      Currently selected speed preset
 * @param labelFor      Returns the localized display name for a speed preset
 * @param onSelected    Called with the newly selected preset
 */
@Composable
private fun SpeedRow(
    label: String,
    selected: NavSpeed,
    labelFor: (NavSpeed) -> String,
    onSelected: (NavSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, fontSize = 24.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavSpeed.entries.forEach { speed ->
                FilterChip(
                    selected = speed == selected,
                    onClick = { onSelected(speed) },
                    label = { Text(labelFor(speed), fontSize = 24.sp) },
                    modifier = Modifier.height(56.dp),
                )
            }
        }
    }
}

/**
 * Label on the left, "−  value  +" on the right. Used for all numeric settings.
 *
 * A generic stepper row: the current value is displayed as pre-formatted text
 * between a minus and a plus button. The caller supplies the current value
 * text and the increment/decrement callbacks, so this row is value-agnostic.
 *
 * @param label         Name of the setting
 * @param valueText     Pre-formatted current value (e.g. "30 s", "1.5 m")
 * @param enabled       Whether the stepper buttons are enabled (only enabled when the
 *                      parent setting is on)
 * @param onMinus       Called when the minus button is pressed
 * @param onPlus        Called when the plus button is pressed
 */
@Composable
private fun SettingStepperRow(
    label: String,
    valueText: String,
    enabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 24.sp, modifier = Modifier.weight(1f))
        Button(
            onClick = onMinus,
            enabled = enabled,
            modifier = Modifier.height(40.dp),
        ) { Text("−", fontSize = 24.sp) }
        Text(valueText, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Button(
            onClick = onPlus,
            enabled = enabled,
            modifier = Modifier.height(40.dp),
        ) { Text("+", fontSize = 24.sp) }
    }
}

/**
 * Label on the left, dropdown with all Temi locations on the right.
 * A text field for choosing the home location.
 *
 * @param label         Name of the setting
 * @param selectedId    ID of the currently selected home location
 * @param enabled       Whether the field is editable (only when auto-return is on)
 * @param onSelected    Called with the new home location id when it changes
 */
@Composable
private fun HomeLocationRow(
    label: String,
    selectedId: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var text by rememberSaveable(selectedId) { mutableStateOf(selectedId) }

    // Save the entry only if it is non-blank and actually changed
    fun save() {
        if (text.isNotBlank() && text.trim() != selectedId) onSelected(text)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 24.sp, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(fontSize = 24.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(onDone = {
                    save()
                    focusManager.clearFocus()
                }),
            modifier =
                Modifier
                    .weight(1f)
                    .onFocusChanged { if (!it.isFocused) save() },
        )
    }
}

/**
 * Function for Creating a MapCalibration block with inputs and confirmation
 *
 * Editable form for the map calibration values:
 *      - origin (x, y)
 *      - scale (pixels per meter)
 *      - anchor yaw (degrees)
 *
 * Empty fields fall back to the current [calibration] value (shown as
 * placeholder); invalid text marks the field as error and disables saving.
 *
 * @param calibration   Current calibration, used as fallback and placeholder
 * @param onSave        Called with the validated new [MapCalibration]
 */
@Composable
fun CalibrationSection(
    calibration: MapCalibration,
    onSave: (MapCalibration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val strings = LocalStrings.current

    var originX by rememberSaveable { mutableStateOf("") }
    var originY by rememberSaveable { mutableStateOf("") }
    var pxPerMeter by rememberSaveable { mutableStateOf("") }
    var anchorYaw by rememberSaveable { mutableStateOf("") }

    // keep old value, if invalid text -> null (shown as error)
    fun parse(
        text: String,
        fallback: Float,
    ): Float? = if (text.isBlank()) fallback else text.replace(',', '.').toFloatOrNull()

    val newX = parse(originX, calibration.originPx.x)
    val newY = parse(originY, calibration.originPx.y)
    val newScale = parse(pxPerMeter, calibration.pxPerMeter)?.takeIf { it > 0f }
    val newYaw = parse(anchorYaw, calibration.anchorYaw)

    // Saving is only possible when every field is valid.
    val allValid = newX != null && newY != null && newScale != null && newYaw != null

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.calibrationHeader, style = MaterialTheme.typography.titleLarge, fontSize = 32.sp)

        // Origin (x and y in one row)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = originX,
                onValueChange = { originX = it },
                label = { Text(strings.originXText, fontSize = 24.sp) },
                placeholder = { Text(calibration.originPx.x.toString()) },
                isError = newX == null,
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = originY,
                onValueChange = { originY = it },
                label = { Text(strings.originYText, fontSize = 24.sp) },
                placeholder = { Text(calibration.originPx.y.toString()) },
                isError = newY == null,
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.weight(1f),
            )
        }

        // Scale in pixels per meter
        OutlinedTextField(
            value = pxPerMeter,
            onValueChange = { pxPerMeter = it },
            label = { Text(strings.pxPerMeterText, fontSize = 24.sp) },
            placeholder = { Text(calibration.pxPerMeter.toString()) },
            isError = newScale == null,
            supportingText = { if (newScale == null) Text(strings.pxPerMeterWarning) },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Anchor yaw in degrees
        OutlinedTextField(
            value = anchorYaw,
            onValueChange = { anchorYaw = it },
            label = { Text(strings.anchorYawText, fontSize = 24.sp) },
            placeholder = { Text(calibration.anchorYaw.toString()) },
            isError = newYaw == null,
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            enabled = allValid,
            onClick = {
                onSave(
                    MapCalibration(
                        originPx = Offset(newX!!, newY!!),
                        pxPerMeter = newScale!!,
                        anchorYaw = newYaw!!,
                    ),
                )
                // clear fields so that placeholders show new values
                originX = ""
                originY = ""
                pxPerMeter = ""
                anchorYaw = ""
                focusManager.clearFocus()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp),
        ) {
            Text(strings.saveCalibrationButton, fontSize = 28.sp)
        }
    }
}

/**
 * Function for displaying the map section
 *
 * Shows a preview with pixel dimensions of the currently loaded map (if available).
 * The user can pick a new map via the system file picker. The Image is then forwarded via
 * [onNewMapPicked].
 * It is also possible to reset the map, which requires a confirmation via [onMapReset].
 *
 * @param map               The current map image
 * @param onMapReset        Called when the user confirms the reset
 * @param onNewMapPicked    Called with the content URI of the picked image
 */
@Composable
fun MapSection(
    map: ImageBitmap?,
    onMapReset: () -> Unit,
    onNewMapPicked: (Uri) -> Unit,
) {
    val strings = LocalStrings.current
    var showResetDialog by remember { mutableStateOf(false) }

    // Opens the system file picker. returns null if the user cancels
    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let(onNewMapPicked)
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.mapSectionTitle, style = MaterialTheme.typography.titleLarge, fontSize = 32.sp)

        // Preview of the map currently in use
        if (map != null) {
            Image(
                bitmap = map,
                contentDescription = strings.mapSectionTitle,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline),
            )
            Text("${map.width} × ${map.height} px", fontSize = 20.sp)
        } else {
            CircularProgressIndicator()
        }

        Text(strings.mapSectionHint, fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { picker.launch("image/png") },
                modifier = Modifier.height(64.dp),
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.mapChooseButton, fontSize = 24.sp)
            }

            // Reset the map (opens a confirmation dialog)
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.height(64.dp),
            ) {
                Text(strings.mapResetButton, fontSize = 24.sp)
            }
        }
    }

    // Confirmation dialog for the map reset
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings.mapResetTitle, fontSize = 28.sp) },
            text = { Text(strings.mapResetMessage, fontSize = 22.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onMapReset()
                }) {
                    Text(strings.mapConfirm, fontSize = 22.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.confirmNo, fontSize = 22.sp)
                }
            },
        )
    }
}

/**
 * Function for UI block for editing the labels (display name and aliases) of all
 * destinations.
 *
 * Renders one [DestinationLabelRow] per destination, prefilled from the saved
 * [labels] map. Saving a row forwards the new display name and aliases via
 * the corresponding callbacks; resetting restores the destination's default
 * labels.
 *
 * @param destinations          List of destinations to edit the label
 * @param labels                Saved labels per destination id, used to prefill the fields
 * @param onDisplayNameSave     Called with (destinationId, newDisplayName)
 * @param onAliasesSave         Called with (destinationId, newAliases)
 * @param onReset               Called with a destination id to reset its given labels
 */
@Composable
fun DestinationLabelSection(
    destinations: List<Destination>,
    labels: Map<String, DestinationLabels>,
    onDisplayNameSave: (String, String) -> Unit,
    onAliasesSave: (String, List<String>) -> Unit,
    onReset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.destinationHeader, style = MaterialTheme.typography.titleLarge, fontSize = 32.sp)
        destinations.forEach { dest ->
            DestinationLabelRow(
                id = dest.id,
                saved = labels[dest.id],
                onDisplayNameSave = { onDisplayNameSave(dest.id, it) },
                onAliasesSave = { onAliasesSave(dest.id, it) },
                onReset = { onReset(dest.id) },
            )
        }
    }
}

/**
 * Edit form for a single destination's labels.
 *
 * Shows the raw destination id (read-only) and editable fields for the
 * display name and the aliases.
 * The fields are initialized again whenever the saved labels change (e.g. after save or reset).
 *
 * @param id                Raw id of the destination, shown as heading
 * @param saved             Currently saved labels for this destination, or `null` if none
 * @param onDisplayNameSave Called with the new display name when "Save" is pressed
 * @param onAliasesSave     Called with the parsed alias list when "Save" is pressed
 * @param onReset           Called when "Reset" is pressed to restore default labels
 */
@Composable
private fun DestinationLabelRow(
    id: String,
    saved: DestinationLabels?,
    onDisplayNameSave: (String) -> Unit,
    onAliasesSave: (List<String>) -> Unit,
    onReset: () -> Unit,
) {
    val strings = LocalStrings.current

    // key on saved values so the fields refresh after save/reset
    var name by rememberSaveable(id, saved?.displayName) { mutableStateOf(saved?.displayName ?: id) }
    var aliases by rememberSaveable(id, saved?.aliases) {
        mutableStateOf(saved?.aliases.orEmpty().joinToString(", "))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(id, style = MaterialTheme.typography.labelLarge, fontSize = 28.sp)
        Text(strings.destinationDisplayName, fontSize = 24.sp)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { strings.destinationDisplayName },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(strings.destinationAliases, fontSize = 24.sp)
        // Aliases are entered comma-separated; e.g. "Rob-Lab, Robotic"
        OutlinedTextField(
            value = aliases,
            onValueChange = { aliases = it },
            label = { strings.destinationAliases },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onDisplayNameSave(name)
                    onAliasesSave(aliases.split(',').map { it.trim() }.filter { it.isNotEmpty() })
                },
                content = { Text(strings.destinationButtonSave, fontSize = 24.sp) },
                modifier = Modifier.height(72.dp),
            )

            // Reset: restore this destination's default labels
            OutlinedButton(
                onClick = onReset,
                content = { Text(strings.destinationButtonReset, fontSize = 24.sp) },
                modifier = Modifier.height(72.dp),
            )
        }
    }
}
