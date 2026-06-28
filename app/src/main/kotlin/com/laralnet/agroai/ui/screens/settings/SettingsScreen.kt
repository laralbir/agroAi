package com.laralnet.agroai.ui.screens.settings

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.domain.model.HuggingFaceCredential
import com.laralnet.agroai.aimodel.domain.model.ModelVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToModels: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val hfCredential by viewModel.hfCredential.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val calendarAccount by viewModel.calendarAccount.collectAsState()
    val context = LocalContext.current
    var showHfDisconnectDialog by remember { mutableStateOf(false) }
    var showCalendarChangeDialog by remember { mutableStateOf<String?>(null) } // pending new email
    var calendarEmailInput by remember { mutableStateOf("") }

    // Launch Chrome Custom Tab when the ViewModel requests it
    LaunchedEffect(Unit) {
        viewModel.browserLaunchEvent.collect { uri ->
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
                .launchUrl(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Theme
            SettingsSectionTitle(text = stringResource(R.string.settings_theme))
            ThemeSelector(selected = themeMode, onSelected = viewModel::setThemeMode)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Language
            SettingsSectionTitle(text = stringResource(R.string.settings_language))
            LanguageSelector()

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // AI Model
            SettingsSectionTitle(text = stringResource(R.string.settings_ai_model))
            SettingsItem(
                icon = Icons.Default.SmartToy,
                title = stringResource(R.string.settings_model_change),
                subtitle = activeModel?.displayName
                    ?: stringResource(R.string.settings_model_none),
                onClick = onNavigateToModels
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // External APIs
            SettingsSectionTitle(text = stringResource(R.string.settings_apis))

            // HuggingFace OAuth item
            HuggingFaceConnectionItem(
                credential = hfCredential,
                onConnect = viewModel::connectHuggingFace,
                onDisconnect = { showHfDisconnectDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Google Calendar account
            SettingsSectionTitle(text = stringResource(R.string.settings_calendar))
            CalendarAccountItem(
                account = calendarAccount,
                emailInput = calendarEmailInput,
                onEmailInputChange = { calendarEmailInput = it },
                onSave = {
                    val newEmail = calendarEmailInput.trim()
                    if (newEmail.isNotBlank()) {
                        if (calendarAccount != null && calendarAccount != newEmail) {
                            showCalendarChangeDialog = newEmail
                        } else {
                            viewModel.setCalendarAccount(newEmail)
                            calendarEmailInput = ""
                        }
                    }
                },
                onClear = {
                    viewModel.clearCalendarAccount()
                    calendarEmailInput = ""
                }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Prompt editor
            SettingsSectionTitle(text = stringResource(R.string.settings_ai_prompts))
            SettingsItem(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.settings_prompt_editor),
                subtitle = stringResource(R.string.settings_prompt_editor_subtitle),
                onClick = { /* navigate to prompt editor */ }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Help & Support
            SettingsSectionTitle(text = stringResource(R.string.settings_help))
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = stringResource(R.string.settings_user_guide),
                subtitle = stringResource(R.string.settings_user_guide_subtitle),
                onClick = {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setUrlBarHidingEnabled(true)
                        .build()
                        .launchUrl(context, android.net.Uri.parse("https://laralbir.github.io/agroAi/"))
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showHfDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showHfDisconnectDialog = false },
            icon = { Icon(Icons.Default.LinkOff, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_hf_disconnect_title)) },
            text = { Text(stringResource(R.string.settings_hf_disconnect_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.disconnectHuggingFace()
                    showHfDisconnectDialog = false
                }) { Text(stringResource(R.string.settings_hf_disconnect_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showHfDisconnectDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Calendar account change dialog (3 options)
    showCalendarChangeDialog?.let { newEmail ->
        AlertDialog(
            onDismissRequest = { showCalendarChangeDialog = null },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_calendar_change_title)) },
            text = { Text(stringResource(R.string.settings_calendar_change_body, calendarAccount ?: "", newEmail)) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.migrateAndSetCalendarAccount(calendarAccount ?: "", newEmail)
                            calendarEmailInput = ""
                            showCalendarChangeDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.settings_calendar_change_migrate)) }
                    OutlinedButton(
                        onClick = {
                            viewModel.setCalendarAccount(newEmail)
                            calendarEmailInput = ""
                            showCalendarChangeDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.settings_calendar_change_lose)) }
                    TextButton(
                        onClick = { showCalendarChangeDialog = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.btn_cancel)) }
                }
            },
            dismissButton = {}
        )
    }

}

@Composable
private fun HuggingFaceConnectionItem(
    credential: HuggingFaceCredential?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_hf_account)) },
        supportingContent = {
            Text(
                text = if (credential != null) {
                    if (credential.username.isNotBlank())
                        stringResource(R.string.settings_hf_connected_as, credential.username)
                    else
                        stringResource(R.string.settings_hf_connected)
                } else {
                    stringResource(R.string.settings_hf_not_connected)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingContent = {
            if (credential != null && credential.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = credential.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = if (credential != null) Icons.Default.AccountCircle else Icons.Default.Key,
                    contentDescription = null,
                    tint = if (credential != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            if (credential != null) {
                TextButton(onClick = onDisconnect) {
                    Text(stringResource(R.string.settings_hf_disconnect))
                }
            } else {
                Button(onClick = onConnect) {
                    Text(stringResource(R.string.settings_hf_connect))
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CalendarAccountItem(
    account: String?,
    emailInput: String,
    onEmailInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_calendar_account)) },
            supportingContent = {
                Text(
                    if (account != null)
                        stringResource(R.string.settings_calendar_connected_as, account)
                    else
                        stringResource(R.string.settings_calendar_not_connected),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (account != null) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                if (account != null) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.settings_calendar_disconnect))
                    }
                }
            }
        )
        if (account == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = onEmailInputChange,
                    placeholder = { Text(stringResource(R.string.settings_calendar_email_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onSave,
                    enabled = emailInput.contains("@")
                ) { Text(stringResource(R.string.settings_calendar_save)) }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelected: (ThemeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                label = {
                    Text(
                        when (mode) {
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun LanguageSelector() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val current by viewModel.languageCode.collectAsState()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LanguageMode.entries.forEach { mode ->
            FilterChip(
                selected = current == mode.name,
                onClick = { viewModel.setLanguageMode(mode) },
                label = {
                    Text(
                        when (mode) {
                            LanguageMode.ENGLISH -> stringResource(R.string.settings_language_en)
                            LanguageMode.SPANISH -> stringResource(R.string.settings_language_es)
                            LanguageMode.SYSTEM -> stringResource(R.string.settings_language_system)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
    )
}
