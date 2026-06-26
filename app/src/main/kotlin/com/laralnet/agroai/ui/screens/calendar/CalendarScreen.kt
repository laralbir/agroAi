package com.laralnet.agroai.ui.screens.calendar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.ui.screens.treatment.resolveLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onNavigateToTreatmentDetail: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    if (!hasPermission) {
        CalendarPermissionScreen(onGrant = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_calendar)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthHeader(
                currentMonth = uiState.currentMonth,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
            MonthGrid(
                currentMonth = uiState.currentMonth,
                selectedDay = uiState.selectedDay,
                daysWithTreatments = uiState.treatmentsByDay.keys,
                onDaySelected = viewModel::selectDay
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DayTreatmentList(
                selectedDay = uiState.selectedDay,
                treatments = uiState.selectedDayTreatments,
                onTreatmentClick = onNavigateToTreatmentDetail
            )
        }
    }
}

@Composable
private fun CalendarPermissionScreen(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                stringResource(R.string.calendar_permission_rationale),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onGrant) {
                Text(stringResource(R.string.calendar_grant_permission))
            }
        }
    }
}

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun MonthGrid(
    currentMonth: YearMonth,
    selectedDay: LocalDate,
    daysWithTreatments: Set<LocalDate>,
    onDaySelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    // Shift so week starts on Monday (1=Mon … 7=Sun → offset 0..6)
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val today = LocalDate.now()

    // Day-of-week headers (Mon–Sun)
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        repeat(7) { idx ->
            val dayName = java.time.DayOfWeek.of((idx + 1).coerceIn(1, 7))
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .take(2).uppercase()
            Text(
                text = dayName,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(Modifier.padding(horizontal = 4.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = currentMonth.atDay(dayNumber)
                        val isSelected = date == selectedDay
                        val isToday = date == today
                        val hasTreatments = date in daysWithTreatments
                        DayCell(
                            day = dayNumber,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasTreatments = hasTreatments,
                            onClick = { onDaySelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasTreatments: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (hasTreatments) {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun DayTreatmentList(
    selectedDay: LocalDate,
    treatments: List<Treatment>,
    onTreatmentClick: (String) -> Unit
) {
    val context = LocalContext.current
    val dateLabel = selectedDay.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

    Text(
        text = dateLabel,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (treatments.isEmpty()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.calendar_no_treatments_day),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(treatments, key = { it.id }) { treatment ->
            TreatmentCalendarCard(
                treatment = treatment,
                onClick = { onTreatmentClick(treatment.id) },
                context = context
            )
        }
    }
}

@Composable
private fun TreatmentCalendarCard(
    treatment: Treatment,
    onClick: () -> Unit,
    context: android.content.Context
) {
    val statusColor = when (treatment.status) {
        TreatmentStatus.DONE -> MaterialTheme.colorScheme.tertiary
        TreatmentStatus.PENDING -> MaterialTheme.colorScheme.primary
        TreatmentStatus.SKIPPED -> MaterialTheme.colorScheme.error
        TreatmentStatus.RESCHEDULED -> MaterialTheme.colorScheme.secondary
    }

    val timeLabel = treatment.scheduledAt
        .atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    treatment.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    treatment.type.resolveLabel(context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                timeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
