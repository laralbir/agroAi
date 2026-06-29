package com.laralnet.agroai.ui.screens.workerlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.domain.model.WorkerRun
import com.laralnet.agroai.ui.components.AppTopBar
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerLogScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: WorkerLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.worker_log_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.runs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.worker_log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.runs, key = { it.id }) { run ->
                    WorkerRunCard(
                        run = run,
                        onClick = { onNavigateToDetail(run.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerRunCard(run: WorkerRun, onClick: () -> Unit) {
    val dateStr = remember(run.timestamp) {
        val ldt = LocalDateTime.ofInstant(run.timestamp, ZoneId.systemDefault())
        ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm"))
    }
    val durationStr = remember(run.durationMs) {
        when {
            run.durationMs < 1000 -> "${run.durationMs} ms"
            run.durationMs < 60_000 -> "${run.durationMs / 1000} s"
            else -> "${run.durationMs / 60_000} min"
        }
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    run.plantationName ?: stringResource(R.string.worker_log_all_plantations),
                    style = MaterialTheme.typography.titleSmall
                )
            },
            supportingContent = {
                Text(
                    "$dateStr · $durationStr",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "🤖 ${run.actionsCreated}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.worker_log_actions_created),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}
