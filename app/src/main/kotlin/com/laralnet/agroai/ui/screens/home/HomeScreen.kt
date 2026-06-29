package com.laralnet.agroai.ui.screens.home

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.infrastructure.worker.PlantationHealthWorker
import com.laralnet.agroai.ui.components.AppTopBar
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.ui.screens.treatment.resolveLabel
import com.laralnet.agroai.weather.domain.model.DailyForecast
import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.domain.model.WeatherData
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlantations: () -> Unit,
    onNavigateToPlantationDetail: (String) -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToTreatmentDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val plantations by viewModel.plantations.collectAsState()
    val hasActiveModel by viewModel.hasActiveModel.collectAsState()
    val todayTreatments by viewModel.todayTreatments.collectAsState()
    val upcomingTreatments by viewModel.upcomingTreatments.collectAsState()
    val homeWeather by viewModel.homeWeather.collectAsState()
    val workerNextRunMs by viewModel.workerNextRunMs.collectAsState()
    val context = LocalContext.current

    var showWeatherSheet by remember { mutableStateOf(false) }

    if (showWeatherSheet && homeWeather != null) {
        WeatherDetailSheet(
            weather = homeWeather!!,
            onDismiss = { showWeatherSheet = false }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { PlantationHealthWorker.scheduleOneTime(context) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_review_now)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToPlantations,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_add_plantation)) }
            )
        }
    ) { paddingValues ->
        if (plantations.isEmpty()) {
            EmptyHomeState(
                hasActiveModel = hasActiveModel,
                onNavigateToModels = onNavigateToModels,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 88.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (!hasActiveModel) {
                    item { NoModelBanner(onClick = onNavigateToModels) }
                }
                item {
                    Text(
                        text = stringResource(R.string.home_welcome),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                }

                homeWeather?.let { weather ->
                    item {
                        HomeWeatherWidget(
                            weather = weather,
                            onClick = { showWeatherSheet = true }
                        )
                    }
                }

                workerNextRunMs?.let { nextMs ->
                    item { WorkerCountdownBadge(nextRunMs = nextMs) }
                }

                item {
                    Text(
                        stringResource(R.string.home_today_treatments),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (todayTreatments.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.home_no_today_treatments),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(todayTreatments, key = { "today_${it.id}" }) { treatment ->
                        UpcomingTreatmentCard(
                            treatment = treatment,
                            onClick = { onNavigateToTreatmentDetail(treatment.id) }
                        )
                    }
                }

                if (upcomingTreatments.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.home_upcoming_treatments),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(upcomingTreatments, key = { "upcoming_${it.id}" }) { treatment ->
                        UpcomingTreatmentCard(
                            treatment = treatment,
                            onClick = { onNavigateToTreatmentDetail(treatment.id) }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                items(plantations, key = { it.id }) { plantation ->
                    PlantationSummaryCard(
                        plantation = plantation,
                        onClick = { onNavigateToPlantationDetail(plantation.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerCountdownBadge(nextRunMs: Long) {
    val remainingMs = (nextRunMs - System.currentTimeMillis()).coerceAtLeast(0)
    val hours = remainingMs / 3_600_000
    val minutes = (remainingMs % 3_600_000) / 60_000
    val text = if (hours == 0L && minutes < 5)
        stringResource(R.string.home_worker_next_run_soon)
    else
        stringResource(R.string.home_worker_next_run, hours, minutes)

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HomeWeatherWidget(weather: WeatherData, onClick: () -> Unit) {
    val current = weather.current ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(current.condition.toEmoji(), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "%.1f°C".format(current.temperatureCelsius),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(R.string.weather_humidity, current.humidity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(
                        R.string.weather_wind,
                        "%.0f".format(current.windSpeedKmh),
                        current.windDirection
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                if (current.precipitationMm > 0) {
                    Text(
                        stringResource(R.string.weather_precipitation, "%.1f".format(current.precipitationMm)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherDetailSheet(weather: WeatherData, onDismiss: () -> Unit) {
    val current = weather.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.weather_detail_title),
                style = MaterialTheme.typography.titleLarge
            )
            if (current != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column {
                        Text(
                            "%.1f°C".format(current.temperatureCelsius),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            stringResource(R.string.weather_feels_like, "%.1f".format(current.feelsLikeCelsius)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        current.condition.toEmoji(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.weather_humidity, current.humidity),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(
                            R.string.weather_wind,
                            "%.0f".format(current.windSpeedKmh),
                            current.windDirection
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (current.precipitationMm > 0) {
                        Text(
                            stringResource(R.string.weather_precipitation, "%.1f".format(current.precipitationMm)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            val next3 = weather.forecast.take(3)
            if (next3.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    stringResource(R.string.weather_3day_forecast),
                    style = MaterialTheme.typography.titleSmall
                )
                next3.forEach { day -> ForecastRow(day) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ForecastRow(day: DailyForecast) {
    val zone = ZoneId.systemDefault()
    val label = day.date.atZone(zone).toLocalDate()
        .format(DateTimeFormatter.ofPattern("EEE d MMM"))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(day.condition.toEmoji(), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            "%.0f° / %.0f°".format(day.maxTempCelsius, day.minTempCelsius),
            style = MaterialTheme.typography.bodySmall
        )
        if (day.precipitationProbability > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                "💧${day.precipitationProbability}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingTreatmentCard(treatment: Treatment, onClick: () -> Unit) {
    val context = LocalContext.current
    val statusColor = when (treatment.status) {
        TreatmentStatus.PENDING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(treatment.title) },
            supportingContent = {
                Text(
                    "${treatment.type.resolveLabel(context)} · ${relativeDate(treatment.scheduledAt, context)}",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            trailingContent = {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        stringResource(R.string.treatment_status_pending),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
    }
}

private fun relativeDate(scheduledAt: Instant, context: Context): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val date = scheduledAt.atZone(zone).toLocalDate()
    val time = LocalDateTime.ofInstant(scheduledAt, zone)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return when (val days = ChronoUnit.DAYS.between(today, date).toInt()) {
        0 -> time
        1 -> "${context.getString(R.string.date_tomorrow)} · $time"
        in 2..13 -> "${context.getString(R.string.date_in_n_days, days)} · $time"
        else -> LocalDateTime.ofInstant(scheduledAt, zone)
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT))
    }
}

fun WeatherCondition.toEmoji() = when (this) {
    WeatherCondition.CLEAR -> "☀️"
    WeatherCondition.PARTLY_CLOUDY -> "⛅"
    WeatherCondition.CLOUDY -> "🌥️"
    WeatherCondition.OVERCAST -> "☁️"
    WeatherCondition.LIGHT_RAIN -> "🌦️"
    WeatherCondition.MODERATE_RAIN -> "🌧️"
    WeatherCondition.HEAVY_RAIN -> "🌧️"
    WeatherCondition.STORM -> "⛈️"
    WeatherCondition.SNOW -> "❄️"
    WeatherCondition.FROST -> "🌨️"
    WeatherCondition.FOG -> "🌫️"
    WeatherCondition.HAIL -> "🌩️"
    WeatherCondition.WINDY -> "💨"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantationSummaryCard(plantation: Plantation, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = plantation.type.defaultIconEmoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = plantation.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${plantation.areaSqMeters.toInt()} m²  •  ${plantation.plants.size} plant types",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plantation.location.displayAddress.isNotBlank()) {
                    Text(
                        text = plantation.location.displayAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun NoModelBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_no_model_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.home_no_model_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.home_download_model))
            }
        }
    }
}

@Composable
private fun EmptyHomeState(
    hasActiveModel: Boolean,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasActiveModel) {
            NoModelBanner(onClick = onNavigateToModels)
            Spacer(Modifier.height(24.dp))
        }
        Icon(
            imageVector = Icons.Default.Agriculture,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_no_plantations),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
