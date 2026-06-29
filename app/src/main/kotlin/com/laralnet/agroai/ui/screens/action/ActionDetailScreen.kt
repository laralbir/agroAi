package com.laralnet.agroai.ui.screens.action

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.ui.components.AppTopBar
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActionDetailViewModel = hiltViewModel()
) {
    val action by viewModel.action.collectAsState()
    val navigateUp by viewModel.navigateUp.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completionNotes by remember { mutableStateOf("") }

    LaunchedEffect(navigateUp) {
        if (navigateUp) onNavigateBack()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.action_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        action?.let { a ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionInfoCard(a)

                if (a.status == ActionStatus.PENDING) {
                    Button(
                        onClick = { showCompleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_mark_done))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.action_delete_title)) },
            text = { Text(stringResource(R.string.action_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text(stringResource(R.string.action_complete_title)) },
            text = {
                OutlinedTextField(
                    value = completionNotes,
                    onValueChange = { completionNotes = it },
                    label = { Text(stringResource(R.string.action_complete_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.complete(completionNotes)
                    showCompleteDialog = false
                }) { Text(stringResource(R.string.action_mark_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun ActionInfoCard(action: PlantationAction) {
    val dateStr = remember(action.scheduledAt) {
        val ldt = LocalDateTime.ofInstant(action.scheduledAt, ZoneId.systemDefault())
        ldt.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm"))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(actionTypeEmoji(action.actionType), style = MaterialTheme.typography.displaySmall)
                Column {
                    Text(action.title, style = MaterialTheme.typography.titleMedium)
                    ActionStatusBadge(action.status)
                }
            }

            if (action.notes.isNotBlank()) {
                Text(action.notes, style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
            }

            if (action.calendarEventId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        stringResource(R.string.action_synced_calendar, action.calendarAccountEmail ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (action.source == ActionSource.AI) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        stringResource(R.string.action_source_ai),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
