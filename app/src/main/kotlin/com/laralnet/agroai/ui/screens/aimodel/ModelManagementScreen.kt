package com.laralnet.agroai.ui.screens.aimodel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.domain.model.DownloadState

private const val HF_TOKENS_URL = "https://huggingface.co/settings/tokens"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.model_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!uiState.hasHfToken) {
                item(key = "hf_banner") {
                    NoTokenBanner(onGetToken = { uriHandler.openUri(HF_TOKENS_URL) })
                }
            }

            items(uiState.rows, key = { it.variant.name }) { row ->
                ModelVariantCard(
                    row = row,
                    onDownload = { viewModel.onDownloadClick(row.variant) },
                    onActivate = { row.model?.id?.let { viewModel.onActivate(it) } },
                    onDelete = { row.model?.id?.let { viewModel.onDelete(it) } }
                )
            }
        }
    }

    // Large model size warning
    uiState.pendingWarningVariant?.let { variant ->
        AlertDialog(
            onDismissRequest = viewModel::onWarningDismissed,
            title = { Text(stringResource(R.string.model_warning_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.model_warning_message,
                        variant.approximateSizeGb,
                        variant.requiredRamGb
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onWarningConfirmed) {
                    Text(stringResource(R.string.btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onWarningDismissed) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // No HuggingFace token dialog
    uiState.noTokenDialogVariant?.let {
        NoTokenDialog(
            onGetToken = { uriHandler.openUri(HF_TOKENS_URL) },
            onSaveAndDownload = viewModel::onTokenSavedAndDownload,
            onDismiss = viewModel::dismissNoTokenDialog
        )
    }
}

@Composable
private fun NoTokenBanner(onGetToken: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.model_no_token_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.model_no_token_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onGetToken) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.model_get_token),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun NoTokenDialog(
    onGetToken: () -> Unit,
    onSaveAndDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tokenInput by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text(stringResource(R.string.model_no_token_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.model_no_token_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("hf_…") },
                    placeholder = { Text("hf_xxxxxxxxxxxx") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = onGetToken,
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
            Button(
                onClick = { if (tokenInput.isNotBlank()) onSaveAndDownload(tokenInput) },
                enabled = tokenInput.isNotBlank()
            ) {
                Text(stringResource(R.string.model_save_and_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun ModelVariantCard(
    row: ModelManagementViewModel.ModelRowState,
    onDownload: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.variant.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${row.variant.approximateSizeGb} GB disk · ${row.variant.requiredRamGb} GB RAM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ModelStatusChip(row)
            }

            if (row.downloadState == DownloadState.DOWNLOADING) {
                Spacer(Modifier.height(10.dp))
                if (row.downloadProgress > 0) {
                    LinearProgressIndicator(
                        progress = { row.downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            // Log panel: shown while DOWNLOADING (live) or after FAILED (persisted)
            val logText = when {
                row.downloadState == DownloadState.DOWNLOADING -> row.displayStatus
                row.downloadState == DownloadState.FAILED -> row.model?.lastError
                else -> null
            }
            if (logText != null) {
                Spacer(Modifier.height(8.dp))
                DownloadLogPanel(
                    log = logText,
                    isFailed = row.downloadState == DownloadState.FAILED,
                    modelInfoUrl = row.variant.infoUrl
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !row.isAvailable -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.model_coming_soon),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    row.downloadState == DownloadState.NOT_DOWNLOADED ||
                    row.downloadState == DownloadState.FAILED -> {
                        Button(onClick = onDownload) {
                            Text(stringResource(R.string.model_download))
                        }
                    }

                    row.downloadState == DownloadState.DOWNLOADING -> {
                        OutlinedButton(onClick = onDelete) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }

                    row.downloadState == DownloadState.DOWNLOADED && !row.isActive -> {
                        Button(onClick = onActivate) {
                            Text(stringResource(R.string.model_activate))
                        }
                        OutlinedButton(onClick = onDelete) {
                            Text(stringResource(R.string.btn_delete))
                        }
                    }

                    row.isActive -> {
                        OutlinedButton(onClick = onDelete) {
                            Text(stringResource(R.string.btn_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadLogPanel(log: String, isFailed: Boolean, modelInfoUrl: String) {
    val uriHandler = LocalUriHandler.current
    val is403 = log.contains("403")
    val containerColor = if (isFailed)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (isFailed)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    if (isFailed) Icons.Default.ErrorOutline else Icons.Default.Info,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isFailed) "Error — log completo" else "Log de descarga",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }

            // Scrollable monospace log
            val scrollV = rememberScrollState()
            val scrollH = rememberScrollState()
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = TextUnit(10f, TextUnitType.Sp)
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .verticalScroll(scrollV)
                        .horizontalScroll(scrollH)
                        .padding(8.dp)
                )
            }

            if (isFailed && is403 && modelInfoUrl.isNotBlank()) {
                TextButton(
                    onClick = { uriHandler.openUri(modelInfoUrl) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.model_accept_terms),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelStatusChip(row: ModelManagementViewModel.ModelRowState) {
    val (label, color) = when {
        row.isActive -> stringResource(R.string.model_active) to MaterialTheme.colorScheme.primary
        row.downloadState == DownloadState.DOWNLOADED ->
            stringResource(R.string.model_downloaded) to MaterialTheme.colorScheme.secondary
        row.downloadState == DownloadState.DOWNLOADING ->
            stringResource(R.string.model_downloading) to MaterialTheme.colorScheme.tertiary
        row.downloadState == DownloadState.FAILED ->
            stringResource(R.string.model_download_failed) to MaterialTheme.colorScheme.error
        else -> return
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
