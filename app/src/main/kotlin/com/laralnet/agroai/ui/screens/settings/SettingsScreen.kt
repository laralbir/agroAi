package com.laralnet.agroai.ui.screens.settings

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    val aemetApiKey by viewModel.aemetApiKey.collectAsState()
    val hfCredential by viewModel.hfCredential.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val context = LocalContext.current
    var showAemetDialog by remember { mutableStateOf(false) }
    var showHfDisconnectDialog by remember { mutableStateOf(false) }
    var aemetKeyInput by remember(aemetApiKey) { mutableStateOf(aemetApiKey) }

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
            SettingsItem(
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.settings_aemet),
                subtitle = if (aemetApiKey.isBlank()) stringResource(R.string.settings_aemet_not_configured) else stringResource(R.string.settings_aemet_configured),
                onClick = { showAemetDialog = true }
            )

            // HuggingFace OAuth item
            HuggingFaceConnectionItem(
                credential = hfCredential,
                onConnect = viewModel::connectHuggingFace,
                onDisconnect = { showHfDisconnectDialog = true }
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

    if (showAemetDialog) {
        AlertDialog(
            onDismissRequest = { showAemetDialog = false },
            title = { Text(stringResource(R.string.settings_aemet_key)) },
            text = {
                OutlinedTextField(
                    value = aemetKeyInput,
                    onValueChange = { aemetKeyInput = it },
                    label = { Text(stringResource(R.string.settings_aemet_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setAemetApiKey(aemetKeyInput)
                    showAemetDialog = false
                }) { Text(stringResource(R.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAemetDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
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
