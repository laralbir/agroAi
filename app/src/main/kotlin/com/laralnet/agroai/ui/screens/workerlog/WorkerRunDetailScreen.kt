package com.laralnet.agroai.ui.screens.workerlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.laralnet.agroai.ui.components.AppTopBar
import com.laralnet.agroai.ui.components.SimpleMarkdownText
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerRunDetailScreen(
    runId: String,
    onNavigateBack: () -> Unit,
    viewModel: WorkerLogViewModel = hiltViewModel()
) {
    LaunchedEffect(runId) { viewModel.loadDetail(runId) }

    val state by viewModel.detailState.collectAsState()
    val run = state.run

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.worker_run_detail_title)) },
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
        if (run == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dateStr = remember(run.timestamp) {
                            LocalDateTime.ofInstant(run.timestamp, ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm"))
                        }
                        val durationStr = remember(run.durationMs) {
                            when {
                                run.durationMs < 1000 -> "${run.durationMs} ms"
                                run.durationMs < 60_000 -> "${run.durationMs / 1000} s"
                                else -> "${run.durationMs / 60_000} min"
                            }
                        }

                        Text(
                            run.plantationName ?: stringResource(R.string.worker_log_all_plantations),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(dateStr, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        HorizontalDivider()

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🤖 ${run.actionsCreated}", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    stringResource(R.string.worker_log_actions_created),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏱ $durationStr", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    stringResource(R.string.worker_run_duration),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Markdown summary
                Text(
                    stringResource(R.string.worker_run_summary),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    SimpleMarkdownText(
                        text = run.summary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
