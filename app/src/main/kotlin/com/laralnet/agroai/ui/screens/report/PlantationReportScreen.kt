package com.laralnet.agroai.ui.screens.report

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.ui.components.AppTopBar
import com.laralnet.agroai.ui.screens.action.actionSourceEmoji
import com.laralnet.agroai.ui.screens.action.actionTypeEmoji
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantationReportScreen(
    plantationId: String,
    onNavigateBack: () -> Unit,
    viewModel: PlantationReportViewModel = hiltViewModel()
) {
    LaunchedEffect(plantationId) { viewModel.load(plantationId) }

    val uiState by viewModel.uiState.collectAsState()
    val plantationName by viewModel.plantationName.collectAsState()
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(stringResource(R.string.report_title))
                        if (plantationName.isNotBlank()) {
                            Text(
                                plantationName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.report_filter)
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, uiState.exportContent)
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(R.string.report_export_subject, plantationName)
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.report_export)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Active type filter chip
            if (uiState.filterActionType != null) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setActionTypeFilter(null) },
                            label = {
                                Text(
                                    "${actionTypeEmoji(uiState.filterActionType!!)} ${uiState.filterActionType!!.name.lowercase().replaceFirstChar { it.uppercase() }}"
                                )
                            }
                        )
                    }
                }
            }

            // Pending section
            item {
                SectionHeader(
                    title = stringResource(R.string.report_section_pending, uiState.pendingActions.size)
                )
            }

            if (uiState.pendingActions.isEmpty()) {
                item {
                    EmptyHint(stringResource(R.string.report_empty_pending))
                }
            } else {
                items(uiState.pendingActions, key = { it.id }) { action ->
                    ReportActionCard(action, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            // History section
            item {
                SectionHeader(
                    title = stringResource(R.string.report_section_history, uiState.completedActions.size),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.completedActions.isEmpty()) {
                item {
                    EmptyHint(stringResource(R.string.report_empty_history))
                }
            } else {
                items(uiState.completedActions, key = { it.id }) { action ->
                    ReportActionCard(action, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
        }
    }

    if (showFilterSheet) {
        ReportFilterSheet(
            currentType = uiState.filterActionType,
            onTypeSelected = { viewModel.setActionTypeFilter(it) },
            onClear = { viewModel.clearFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun ReportActionCard(action: PlantationAction, modifier: Modifier = Modifier) {
    val dateStr = remember(action.scheduledAt) {
        val ldt = LocalDateTime.ofInstant(action.scheduledAt, ZoneId.systemDefault())
        ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }
    val sourceEmoji = actionSourceEmoji(action.source)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (action.status == ActionStatus.DONE)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        ListItem(
            headlineContent = { Text(action.title, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = {
                Text(
                    "${actionTypeEmoji(action.actionType)} $dateStr  $sourceEmoji",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            trailingContent = {
                if (action.status == ActionStatus.DONE) {
                    Text("✓", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary)
                }
            }
        )
        if (action.notes.isNotBlank()) {
            Text(
                action.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFilterSheet(
    currentType: ActionType?,
    onTypeSelected: (ActionType?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.report_filter), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    onClear()
                    onDismiss()
                }) { Text(stringResource(R.string.report_filter_clear)) }
            }

            Text(
                stringResource(R.string.action_type_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = currentType == null,
                        onClick = { onTypeSelected(null) },
                        label = { Text(stringResource(R.string.action_filter_all)) }
                    )
                }
                items(ActionType.entries) { type ->
                    FilterChip(
                        selected = currentType == type,
                        onClick = { onTypeSelected(type) },
                        label = { Text("${actionTypeEmoji(type)} ${type.name.lowercase().replaceFirstChar { it.uppercase() }}") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
