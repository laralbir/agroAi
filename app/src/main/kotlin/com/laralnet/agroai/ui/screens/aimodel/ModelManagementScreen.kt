package com.laralnet.agroai.ui.screens.aimodel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.domain.model.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                LinearProgressIndicator(
                    progress = { row.downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${row.downloadProgress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
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
                        // No action while downloading
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
