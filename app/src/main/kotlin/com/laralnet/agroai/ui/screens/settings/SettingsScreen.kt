package com.laralnet.agroai.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToModels: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val aemetApiKey by viewModel.aemetApiKey.collectAsState()
    val hfToken by viewModel.hfToken.collectAsState()
    var showAemetDialog by remember { mutableStateOf(false) }
    var showHfTokenDialog by remember { mutableStateOf(false) }
    var aemetKeyInput by remember(aemetApiKey) { mutableStateOf(aemetApiKey) }
    var hfTokenInput by remember(hfToken) { mutableStateOf(hfToken) }
    val uriHandler = LocalUriHandler.current

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
                subtitle = "Gemma 3 — select and download",
                onClick = onNavigateToModels
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // External APIs
            SettingsSectionTitle(text = stringResource(R.string.settings_apis))
            SettingsItem(
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.settings_aemet),
                subtitle = if (aemetApiKey.isBlank()) "API key not configured" else "Configured",
                onClick = { showAemetDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Key,
                title = stringResource(R.string.settings_hf_token),
                subtitle = if (hfToken.isBlank()) stringResource(R.string.settings_hf_token_hint) else "Configured",
                onClick = { showHfTokenDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Prompt editor
            SettingsSectionTitle(text = "AI Prompts")
            SettingsItem(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.settings_prompt_editor),
                subtitle = "Customize Gemma analysis prompts",
                onClick = { /* navigate to prompt editor */ }
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showHfTokenDialog) {
        AlertDialog(
            onDismissRequest = { showHfTokenDialog = false },
            icon = { Icon(Icons.Default.Key, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_hf_token)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_hf_token_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = hfTokenInput,
                        onValueChange = { hfTokenInput = it },
                        label = { Text("hf_…") },
                        placeholder = { Text("hf_xxxxxxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { uriHandler.openUri("https://huggingface.co/settings/tokens") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.model_get_token_huggingface))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setHfToken(hfTokenInput)
                    showHfTokenDialog = false
                }) { Text(stringResource(R.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showHfTokenDialog = false }) {
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
