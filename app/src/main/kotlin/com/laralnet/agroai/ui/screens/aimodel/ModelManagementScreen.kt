package com.laralnet.agroai.ui.screens.aimodel

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Open Chrome Custom Tab when the ViewModel requests the browser
    LaunchedEffect(Unit) {
        viewModel.browserLaunchEvent.collect { uri ->
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
                .launchUrl(context, uri)
        }
    }

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
                    NoTokenBanner(onConnect = viewModel::onConnectHuggingFace)
                }
            }

            items(uiState.rows, key = { it.variant.name }) { row ->
                ModelVariantCard(
                    row = row,
                    onDownload = { viewModel.onDownloadClick(row.variant) },
                    onActivate = { row.model?.id?.let { viewModel.onActivate(it) } },
                    onDelete = { row.model?.id?.let { viewModel.onDelete(it) } },
                    onTest = { row.model?.filePath?.let { path ->
                        viewModel.openTestSheet(path, row.variant.displayName)
                    }}
                )
            }
        }
    }

    // Model test bottom sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    uiState.testResult?.let { result ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissTestResult,
            sheetState = sheetState
        ) {
            ModelTestSheet(
                result = result,
                onRun = viewModel::runTest,
                onUpdatePrompt = viewModel::updateTestPrompt
            )
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

    // HuggingFace OAuth dialog (shown when download is requested but no account connected)
    if (uiState.oauthDialogVariant != null) {
        HuggingFaceOAuthDialog(
            connecting = uiState.oauthConnecting,
            onConnect = viewModel::onConnectHuggingFace,
            onDismiss = viewModel::dismissOAuthDialog
        )
    }
}

@Composable
private fun NoTokenBanner(onConnect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.model_no_account_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.model_no_account_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Button(onClick = onConnect) {
                Text(stringResource(R.string.model_connect_hf))
            }
        }
    }
}

@Composable
private fun HuggingFaceOAuthDialog(
    connecting: Boolean,
    onConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!connecting) onDismiss() },
        icon = { Icon(Icons.Default.HowToReg, contentDescription = null) },
        title = { Text(stringResource(R.string.model_oauth_dialog_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (connecting) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.model_oauth_connecting),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = stringResource(R.string.model_oauth_dialog_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (!connecting) {
                Button(onClick = onConnect) {
                    Icon(
                        Icons.Default.HowToReg,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.model_oauth_connect_button))
                }
            }
        },
        dismissButton = {
            if (!connecting) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        }
    )
}

@Composable
private fun ModelVariantCard(
    row: ModelManagementViewModel.ModelRowState,
    onDownload: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = onTest) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.model_test_run))
                        }
                    }

                    row.isActive -> {
                        OutlinedButton(onClick = onDelete) {
                            Text(stringResource(R.string.btn_delete))
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = onTest) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.model_test_run))
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
private fun ModelTestSheet(
    result: ModelManagementViewModel.ModelTestResult,
    onRun: () -> Unit,
    onUpdatePrompt: (String) -> Unit
) {
    val hasResult = result.response != null || result.error != null
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) { delay(1500); copied = false }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: model name + status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(result.variantDisplayName, style = MaterialTheme.typography.titleLarge)
            when {
                result.isRunning -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.model_test_running),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                result.isSuccess -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.model_test_passed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                result.error != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.model_test_failed_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        HorizontalDivider()

        // Details: file size, path, timings (only when available)
        Text(stringResource(R.string.model_test_details), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TestDetailRow(stringResource(R.string.model_test_disk_size), "%.1f MB".format(result.fileSizeBytes / 1_048_576.0))
            TestDetailRow(stringResource(R.string.model_test_file_path), result.filePath, monospace = true)
            if (result.loadTimeMs > 0) TestDetailRow(stringResource(R.string.model_test_load_time), formatDuration(result.loadTimeMs))
            if (result.inferenceTimeMs > 0) {
                TestDetailRow(stringResource(R.string.model_test_inference_time), formatDuration(result.inferenceTimeMs))
                TestDetailRow(stringResource(R.string.model_test_total_time), formatDuration(result.totalTimeMs))
            }
        }

        HorizontalDivider()

        // Editable prompt + run button
        Text(stringResource(R.string.model_test_prompt), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.OutlinedTextField(
            value = result.prompt,
            onValueChange = onUpdatePrompt,
            enabled = !result.isRunning,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TextUnit(10f, TextUnitType.Sp)
            ),
            label = { Text(stringResource(R.string.model_test_prompt_hint), style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            shape = MaterialTheme.shapes.small
        )
        Button(
            onClick = onRun,
            enabled = !result.isRunning && result.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (result.isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.model_test_running))
            } else {
                Icon(Icons.Default.Science, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (hasResult) stringResource(R.string.btn_retry) else stringResource(R.string.model_test_execute))
            }
        }

        // Result section (only shown after running)
        if (hasResult) {
            HorizontalDivider()

            when {
                result.error != null -> {
                    Text(stringResource(R.string.model_test_error), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = { clipboardManager.setText(AnnotatedString(result.error)); copied = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                            null, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(if (copied) stringResource(R.string.model_test_copied) else stringResource(R.string.model_test_copy_error))
                    }
                }

                result.response != null -> {
                    Text(stringResource(R.string.model_test_response), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(result.response, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TestDetailRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = if (monospace)
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            else
                MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.62f),
            textAlign = TextAlign.End
        )
    }
}

private fun formatDuration(ms: Long): String =
    if (ms < 1000) "$ms ms" else "${"%.2f".format(ms / 1000.0)} s"

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
