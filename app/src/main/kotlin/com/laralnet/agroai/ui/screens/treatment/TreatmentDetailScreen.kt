package com.laralnet.agroai.ui.screens.treatment

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.weather.domain.model.WeatherAlert
import com.laralnet.agroai.weather.domain.model.WeatherAlertType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TreatmentDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TreatmentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isDone, state.isDeleted) {
        if (state.isDone || state.isDeleted) onNavigateBack()
    }
    TreatmentDetailContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onComplete = viewModel::complete,
        onDelete = viewModel::delete
    )
}

@Composable
@VisibleForTesting
internal fun TreatmentDetailContent(
    state: TreatmentDetailState,
    onNavigateBack: () -> Unit,
    onComplete: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var completeNotes by remember { mutableStateOf("") }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text(stringResource(R.string.treatment_btn_complete)) },
            text = {
                OutlinedTextField(
                    value = completeNotes,
                    onValueChange = { completeNotes = it },
                    label = { Text(stringResource(R.string.treatment_complete_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onComplete(completeNotes)
                    showCompleteDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.treatment_delete_confirm_title)) },
            text = { Text(stringResource(R.string.treatment_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.treatment_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                },
                actions = {
                    if (state.treatment?.status == TreatmentStatus.PENDING) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.treatment_btn_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        state.treatment?.let { treatment ->
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp)
                    .testTag("treatment_content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TreatmentInfoCard(treatment = treatment, context = context)

                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        headlineContent = { Text(stringResource(R.string.treatment_scheduled_at)) },
                        supportingContent = { Text(formatInstant(treatment)) }
                    )
                }

                state.weatherAlerts.forEach { alert ->
                    WeatherAlertCard(alert = alert)
                }

                if (treatment.calendarEventId != null) {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .testTag("treatment_calendar_badge")
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            headlineContent = { Text(stringResource(R.string.treatment_in_calendar)) },
                            supportingContent = treatment.calendarAccountEmail?.let {
                                { Text(it) }
                            }
                        )
                    }
                }

                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (treatment.status == TreatmentStatus.PENDING) {
                    Button(
                        onClick = { showCompleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("treatment_complete_btn"),
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.treatment_btn_complete))
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("treatment_loading"),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun TreatmentInfoCard(treatment: Treatment, context: android.content.Context) {
    val statusColor = when (treatment.status) {
        TreatmentStatus.PENDING -> MaterialTheme.colorScheme.secondary
        TreatmentStatus.DONE -> MaterialTheme.colorScheme.primary
        TreatmentStatus.SKIPPED, TreatmentStatus.RESCHEDULED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (treatment.status) {
        TreatmentStatus.PENDING -> stringResource(R.string.treatment_status_pending)
        TreatmentStatus.DONE -> stringResource(R.string.treatment_status_done)
        TreatmentStatus.SKIPPED -> stringResource(R.string.treatment_status_skipped)
        TreatmentStatus.RESCHEDULED -> stringResource(R.string.treatment_status_rescheduled)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("treatment_info_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(treatment.title, style = MaterialTheme.typography.titleLarge)
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                treatment.type.resolveLabel(context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (treatment.description.isNotBlank()) {
                Text(treatment.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatInstant(treatment: Treatment): String {
    val ldt = LocalDateTime.ofInstant(treatment.scheduledAt, ZoneId.systemDefault())
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(ldt)
}

@Composable
private fun WeatherAlertCard(alert: WeatherAlert) {
    val message = stringResource(
        when (alert.type) {
            WeatherAlertType.FROST -> R.string.weather_alert_frost
            WeatherAlertType.HEAVY_RAIN -> R.string.weather_alert_heavy_rain
            WeatherAlertType.STORM -> R.string.weather_alert_storm
            WeatherAlertType.HAIL -> R.string.weather_alert_hail
            WeatherAlertType.SNOW -> R.string.weather_alert_snow
            else -> R.string.weather_alert_generic
        }
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("treatment_weather_alert"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            leadingContent = {
                Text("⚠️", style = MaterialTheme.typography.titleMedium)
            },
            headlineContent = {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }
}
