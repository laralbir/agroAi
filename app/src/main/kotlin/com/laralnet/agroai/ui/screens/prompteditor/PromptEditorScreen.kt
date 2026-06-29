package com.laralnet.agroai.ui.screens.prompteditor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.model.PromptWarningLevel
import com.laralnet.agroai.ui.components.AppTopBar
import java.time.LocalDate

@Composable
fun PromptEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: PromptEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.prompt_saved)

    LaunchedEffect(state.savedOk) {
        if (state.savedOk) {
            snackbarHostState.showSnackbar(savedMsg)
            viewModel.clearSavedOk()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.prompt_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Template selector
                Text(
                    stringResource(R.string.settings_ai_prompts),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.templates.forEach { template ->
                        TemplateCard(
                            template = template,
                            isSelected = template.id == state.selectedId,
                            onClick = { viewModel.selectTemplate(template.id) }
                        )
                    }
                }

                val selected = state.selectedTemplate
                if (selected == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.prompt_select_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    PromptEditorArea(
                        template = selected,
                        content = state.editContent,
                        isModified = state.isModified,
                        isSaving = state.isSaving,
                        onContentChanged = viewModel::onContentChanged,
                        onSave = viewModel::savePrompt,
                        onReset = viewModel::resetPrompt
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (state.showWarningDialog) {
        val template = state.selectedTemplate
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveDialog,
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.prompt_save_dialog_title)) },
            text = {
                Text(
                    when (template?.warningLevel) {
                        PromptWarningLevel.HIGH -> stringResource(R.string.prompt_warning_high)
                        else -> stringResource(R.string.prompt_warning_medium)
                    }
                )
            },
            confirmButton = {
                Button(onClick = viewModel::confirmSave) {
                    Text(stringResource(R.string.prompt_save_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSaveDialog) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetDialog,
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
            title = { Text(stringResource(R.string.prompt_reset_dialog_title)) },
            text = { Text(stringResource(R.string.prompt_reset_dialog_body)) },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.prompt_reset_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResetDialog) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: PromptTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayName = templateDisplayName(template.name)
    val warningLabel = when (template.warningLevel) {
        PromptWarningLevel.LOW -> stringResource(R.string.prompt_warning_level_low)
        PromptWarningLevel.MEDIUM -> stringResource(R.string.prompt_warning_level_medium)
        PromptWarningLevel.HIGH -> stringResource(R.string.prompt_warning_level_high)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 140.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                warningLabel,
                style = MaterialTheme.typography.labelSmall,
                color = when (template.warningLevel) {
                    PromptWarningLevel.HIGH -> MaterialTheme.colorScheme.error
                    PromptWarningLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    PromptWarningLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (template.isCustomized) {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(R.string.prompt_customized),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Composable
private fun templateDisplayName(name: String): String = when (name) {
    "photo_analysis" -> stringResource(R.string.prompt_template_photo_analysis)
    "plantation_health" -> stringResource(R.string.prompt_template_plantation_health)
    "weather_adjustment" -> stringResource(R.string.prompt_template_weather_adjustment)
    else -> name
}

@Composable
private fun PromptEditorArea(
    template: PromptTemplate,
    content: String,
    isModified: Boolean,
    isSaving: Boolean,
    onContentChanged: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    var showPreview by remember(template.id) { mutableStateOf(false) }

    HorizontalDivider()

    // Warning banner
    if (template.warningLevel >= PromptWarningLevel.MEDIUM) {
        Surface(
            color = if (template.warningLevel == PromptWarningLevel.HIGH)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (template.warningLevel == PromptWarningLevel.HIGH)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = when (template.warningLevel) {
                        PromptWarningLevel.HIGH -> stringResource(R.string.prompt_warning_high)
                        else -> stringResource(R.string.prompt_warning_medium)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // Editor
    OutlinedTextField(
        value = content,
        onValueChange = onContentChanged,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        label = { Text(stringResource(R.string.prompt_editor_title)) },
        minLines = 10
    )

    // Preview toggle
    TextButton(
        onClick = { showPreview = !showPreview },
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        Icon(
            if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.prompt_preview_title), style = MaterialTheme.typography.labelMedium)
    }

    AnimatedVisibility(visible = showPreview) {
        PreviewCard(template = template, editContent = content)
    }

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) {
        OutlinedButton(
            onClick = onReset,
            enabled = !isSaving && content != template.defaultContent
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.prompt_reset))
        }
        Button(
            onClick = onSave,
            enabled = !isSaving && isModified
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.prompt_save))
        }
    }
}

@Composable
private fun PreviewCard(template: PromptTemplate, editContent: String) {
    val sampleContext = buildSampleContext(template.name)
    val previewText = if (sampleContext.isBlank()) editContent else "$editContent\n\n$sampleContext"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.prompt_preview_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                previewText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildSampleContext(templateName: String): String {
    val today = LocalDate.now()
    return when (templateName) {
        "photo_analysis" -> """
---
Context:
- Plantation: "Mi Huerta" (Vegetable Garden / Huerta)
- Plant: Tomato — Cherry (50 plants, 0.5 m × 0.3 m rows)
- Location: Murcia, Murcia, Spain
- Date: $today
- Weather: 24 °C · Sunny · Humidity 62% · Wind 12 km/h
- Forecast: Sunny 27/19 °C · Sunny 28/20 °C · Partly cloudy 23/17 °C""".trimIndent()

        "plantation_health" -> """
---
Plantation data:
- Name: "Mi Huerta" (Vegetable Garden / Huerta)
- Plants: Tomato — Cherry (50 plants), Lettuce (30 plants), Pepper (20 plants)
- Location: Murcia, Murcia, Spain
- Date: $today
- Forecast (14 days): Week 1: sunny 25–28 °C / Week 2: rain expected 60% probability""".trimIndent()

        "weather_adjustment" -> """
---
Treatment: Irrigation (RIEGO)
Scheduled: ${today.plusDays(2)}
Plantation: "Mi Olivar" (Olive Grove, Murcia, Spain)
Forecast that day: Rain expected — 15 mm · 80% probability · Wind 25 km/h""".trimIndent()

        else -> ""
    }
}
