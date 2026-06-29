package com.laralnet.agroai.ui.screens.action

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.ui.components.AppTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionListScreen(
    plantationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: ActionListViewModel = hiltViewModel()
) {
    LaunchedEffect(plantationId) { viewModel.load(plantationId) }

    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.action_list_title)) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_create))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            val filters = ActionFilter.entries
            val filterLabels = listOf(
                stringResource(R.string.action_filter_all),
                stringResource(R.string.action_filter_pending),
                stringResource(R.string.action_filter_done),
                stringResource(R.string.action_filter_skipped)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters.size) { i ->
                    FilterChip(
                        selected = uiState.filter == filters[i],
                        onClick = { viewModel.setFilter(filters[i]) },
                        label = { Text(filterLabels[i]) }
                    )
                }
            }

            if (uiState.actions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.action_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.actions, key = { it.id }) { action ->
                        ActionCard(
                            action = action,
                            onClick = { onNavigateToDetail(action.id) },
                            onDelete = { viewModel.deleteAction(action.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateActionDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { type, title, notes, date ->
                viewModel.scheduleAction(type, title, notes, date)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    action: PlantationAction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateStr = remember(action.scheduledAt) {
        val ldt = LocalDateTime.ofInstant(action.scheduledAt, ZoneId.systemDefault())
        ldt.format(DateTimeFormatter.ofPattern("dd MMM · HH:mm"))
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(action.title, style = MaterialTheme.typography.titleSmall) },
            supportingContent = {
                Text(
                    "${actionTypeEmoji(action.actionType)} $dateStr",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionStatusBadge(action.status)
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.action_delete_title)) },
            text = { Text(stringResource(R.string.action_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
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
}

@Composable
internal fun ActionStatusBadge(status: ActionStatus) {
    val (label, color) = when (status) {
        ActionStatus.PENDING -> Pair(
            stringResource(R.string.action_status_pending),
            MaterialTheme.colorScheme.primary
        )
        ActionStatus.DONE -> Pair(
            stringResource(R.string.action_status_done),
            MaterialTheme.colorScheme.tertiary
        )
        ActionStatus.SKIPPED -> Pair(
            stringResource(R.string.action_status_skipped),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateActionDialog(
    onDismiss: () -> Unit,
    onConfirm: (ActionType, String, String, Instant) -> Unit
) {
    var selectedType by remember { mutableStateOf(ActionType.REGAR) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var scheduledDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var typeExpanded by remember { mutableStateOf(false) }

    // Build label list once in composable context
    val typeLabels = ActionType.entries.map { it to actionTypeLabel(it) }

    val dateLabelTomorrow = stringResource(R.string.action_date_tomorrow)
    val dateLabel3days = stringResource(R.string.action_date_3days)
    val dateLabel7days = stringResource(R.string.action_date_7days)
    val dateLabel14days = stringResource(R.string.action_date_14days)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Action type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    val selectedLabel = typeLabels.first { it.first == selectedType }.second
                    OutlinedTextField(
                        value = "${actionTypeEmoji(selectedType)} $selectedLabel",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.action_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeLabels.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text("${actionTypeEmoji(type)} $label") },
                                onClick = {
                                    selectedType = type
                                    if (title.isBlank()) title = label
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.action_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.action_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Date presets
                Text(
                    stringResource(R.string.action_date_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val today = LocalDate.now()
                val dateOptions = listOf(
                    today.plusDays(1) to dateLabelTomorrow,
                    today.plusDays(3) to dateLabel3days,
                    today.plusDays(7) to dateLabel7days,
                    today.plusDays(14) to dateLabel14days
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dateOptions.forEach { (date, label) ->
                        FilterChip(
                            selected = scheduledDate == date,
                            onClick = { scheduledDate = date },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val instant = scheduledDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    onConfirm(selectedType, title.trim(), notes.trim(), instant)
                }
            ) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

internal fun actionTypeEmoji(type: ActionType): String = when (type) {
    ActionType.REGAR -> "💧"
    ActionType.PODAR -> "✂️"
    ActionType.CAVAR -> "⛏️"
    ActionType.FERTILIZAR -> "🌿"
    ActionType.FUMIGAR -> "🌫️"
    ActionType.COSECHAR -> "🌾"
    ActionType.INJERTAR -> "🌱"
    ActionType.TRASPLANTAR -> "🪴"
    ActionType.ABONAR -> "🪵"
    ActionType.AIREAR -> "🌬️"
    ActionType.ACLARAR -> "☀️"
    ActionType.OTRO -> "📋"
}

@Composable
internal fun actionTypeLabel(type: ActionType): String = when (type) {
    ActionType.REGAR -> stringResource(R.string.action_type_regar)
    ActionType.PODAR -> stringResource(R.string.action_type_podar)
    ActionType.CAVAR -> stringResource(R.string.action_type_cavar)
    ActionType.FERTILIZAR -> stringResource(R.string.action_type_fertilizar)
    ActionType.FUMIGAR -> stringResource(R.string.action_type_fumigar)
    ActionType.COSECHAR -> stringResource(R.string.action_type_cosechar)
    ActionType.INJERTAR -> stringResource(R.string.action_type_injertar)
    ActionType.TRASPLANTAR -> stringResource(R.string.action_type_trasplantar)
    ActionType.ABONAR -> stringResource(R.string.action_type_abonar)
    ActionType.AIREAR -> stringResource(R.string.action_type_airear)
    ActionType.ACLARAR -> stringResource(R.string.action_type_aclarar)
    ActionType.OTRO -> stringResource(R.string.action_type_otro)
}
