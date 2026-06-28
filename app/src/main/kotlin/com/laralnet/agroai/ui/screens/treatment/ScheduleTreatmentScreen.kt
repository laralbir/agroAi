package com.laralnet.agroai.ui.screens.treatment

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTreatmentScreen(
    onNavigateBack: () -> Unit,
    onTreatmentScheduled: () -> Unit,
    viewModel: ScheduleTreatmentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.savedId) {
        if (state.savedId != null) onTreatmentScheduled()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var calendarDropdownExpanded by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.selectedDateMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = state.hour,
        initialMinute = state.minute,
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setDateMillis(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setHour(timePickerState.hour)
                    viewModel.setMinute(timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.treatment_schedule_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type selector
            Text(stringResource(R.string.treatment_type_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TreatmentType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.resolveLabel(context)) }
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text(stringResource(R.string.treatment_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text(stringResource(R.string.treatment_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            OutlinedTextField(
                value = formatEpochMillis(state.selectedDateMillis),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.treatment_pick_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            OutlinedTextField(
                value = "%02d:%02d".format(state.hour, state.minute),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.treatment_pick_time)) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
            )

            HorizontalDivider()

            // Calendar toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.treatment_add_to_calendar))
                Switch(
                    checked = state.addToCalendar,
                    onCheckedChange = viewModel::setAddToCalendar
                )
            }

            if (state.addToCalendar) {
                // Email input + load button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = state.calendarEmail,
                        onValueChange = viewModel::setCalendarEmail,
                        label = { Text(stringResource(R.string.treatment_calendar_email)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.loadCalendars()
                        })
                    )
                    FilledTonalButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.loadCalendars()
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = state.calendarEmail.isNotBlank() && !state.isLoadingCalendars
                    ) {
                        if (state.isLoadingCalendars) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.treatment_load_calendars))
                        }
                    }
                }

                // Calendar picker dropdown — visible once calendars are loaded
                if (state.availableCalendars.isNotEmpty()) {
                    val selectedCalendar = state.availableCalendars.firstOrNull { it.id == state.selectedCalendarId }
                    ExposedDropdownMenuBox(
                        expanded = calendarDropdownExpanded,
                        onExpandedChange = { calendarDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCalendar?.displayName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.treatment_select_calendar)) },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = calendarDropdownExpanded,
                            onDismissRequest = { calendarDropdownExpanded = false }
                        ) {
                            state.availableCalendars.forEach { cal ->
                                DropdownMenuItem(
                                    text = { Text(cal.displayName) },
                                    onClick = {
                                        viewModel.selectCalendar(cal.id)
                                        calendarDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (state.error == "no_calendars") {
                    Text(
                        stringResource(R.string.treatment_no_calendars),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (state.error != null && state.error != "no_calendars") {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = viewModel::schedule,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.treatment_btn_schedule))
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

internal fun TreatmentType.resolveLabel(context: Context): String {
    val key = "treatment_${name.lowercase()}"
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else name
}

private fun formatEpochMillis(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
}
