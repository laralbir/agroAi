package com.laralnet.agroai.ui.screens.plantation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.ui.screens.treatment.resolveLabel
import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.domain.model.WeatherData
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantationDetailScreen(
    plantationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToAnalysisWithPlant: (plantationId: String, plantTypeId: String) -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToScheduleTreatment: (String) -> Unit,
    onNavigateToTreatmentDetail: (String) -> Unit,
    viewModel: PlantationDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(plantationId) { viewModel.load(plantationId) }

    val plantation by viewModel.plantation.collectAsState()
    val treatments by viewModel.treatments.collectAsState()
    val weather by viewModel.weather.collectAsState()

    // Edit plant dialog state
    var editingPlant by remember { mutableStateOf<PlantType?>(null) }
    var deletingPlantId by remember { mutableStateOf<String?>(null) }

    editingPlant?.let { plant ->
        EditPlantDialog(
            plant = plant,
            onDismiss = { editingPlant = null },
            onConfirm = { updated ->
                viewModel.updatePlantType(updated)
                editingPlant = null
            }
        )
    }

    deletingPlantId?.let { plantId ->
        AlertDialog(
            onDismissRequest = { deletingPlantId = null },
            title = { Text(stringResource(R.string.plant_delete_confirm_title)) },
            text = { Text(stringResource(R.string.plant_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlantType(plantId)
                    deletingPlantId = null
                }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlantId = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plantation?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAnalysis) {
                        Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.analysis_take_photo))
                    }
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.plantation_edit))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToScheduleTreatment(plantationId) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.treatment_add))
            }
        }
    ) { paddingValues ->
        plantation?.let { p ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp, bottom = 88.dp, start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item { PlantationHeaderCard(plantation = p) }

                val loc = p.location
                if (loc.displayAddress.isNotBlank() || loc.hasCoordinates) {
                    item { PlantationLocationCard(location = loc) }
                }

                weather?.let { w ->
                    item { WeatherCard(weather = w) }
                }

                if (p.plants.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.plantation_plants),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(p.plants, key = { it.id }) { plant ->
                        PlantCard(
                            plant = plant,
                            onAnalyze = { onNavigateToAnalysisWithPlant(plantationId, plant.id) },
                            onEdit = { editingPlant = plant },
                            onDelete = { deletingPlantId = plant.id }
                        )
                    }
                }

                item {
                    Text(
                        stringResource(R.string.plantation_detail_treatments),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (treatments.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.plantation_detail_no_treatments),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(treatments, key = { it.id }) { treatment ->
                        TreatmentCard(
                            treatment = treatment,
                            onClick = { onNavigateToTreatmentDetail(treatment.id) }
                        )
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun PlantationHeaderCard(plantation: Plantation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(plantation.type.defaultIconEmoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(plantation.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${plantation.areaSqMeters.toInt()} m²",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plantation.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        plantation.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantationLocationCard(location: Location) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(stringResource(R.string.plantation_location), style = MaterialTheme.typography.labelLarge)
            }
            if (location.displayAddress.isNotBlank()) {
                Text(location.displayAddress, style = MaterialTheme.typography.bodyMedium)
            }
            if (location.hasCoordinates) {
                Text(
                    "%.5f, %.5f".format(location.latitude, location.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantCard(
    plant: PlantType,
    onAnalyze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val unitsLabel = stringResource(R.string.plantation_units)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Default.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                headlineContent = {
                    Text(
                        if (plant.variety.isNotBlank()) "${plant.name} · ${plant.variety}"
                        else plant.name
                    )
                },
                supportingContent = if (plant.count > 0) {
                    { Text("${plant.count} $unitsLabel") }
                } else null
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAnalyze) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.cd_analyze_plant),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.plant_analyze))
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_plant),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_plant),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherData) {
    val current = weather.current ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.weather_today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    current.condition.toLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "%.1f°C".format(current.temperatureCelsius),
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.weather_humidity, current.humidity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.weather_wind, "%.0f".format(current.windSpeedKmh), current.windDirection),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun WeatherCondition.toLabel(): String = when (this) {
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
private fun TreatmentCard(treatment: Treatment, onClick: () -> Unit) {
    val context = LocalContext.current
    val statusColor = when (treatment.status) {
        TreatmentStatus.PENDING -> MaterialTheme.colorScheme.secondary
        TreatmentStatus.DONE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dateStr = LocalDateTime.ofInstant(treatment.scheduledAt, ZoneId.systemDefault())
        .let { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT).format(it) }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(treatment.title) },
            supportingContent = {
                Text(
                    "${treatment.type.resolveLabel(context)} · $dateStr",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            trailingContent = {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        when (treatment.status) {
                            TreatmentStatus.PENDING -> context.getString(R.string.treatment_status_pending)
                            TreatmentStatus.DONE -> context.getString(R.string.treatment_status_done)
                            TreatmentStatus.SKIPPED -> context.getString(R.string.treatment_status_skipped)
                            TreatmentStatus.RESCHEDULED -> context.getString(R.string.treatment_status_rescheduled)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
    }
}

// ---- Edit plant dialog ----
@Composable
private fun EditPlantDialog(
    plant: PlantType,
    onDismiss: () -> Unit,
    onConfirm: (PlantType) -> Unit
) {
    var name by remember { mutableStateOf(plant.name) }
    var variety by remember { mutableStateOf(plant.variety) }
    var countText by remember { mutableStateOf(if (plant.count > 0) plant.count.toString() else "") }
    var notes by remember { mutableStateOf(plant.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plant_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.plant_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = variety,
                    onValueChange = { variety = it },
                    label = { Text(stringResource(R.string.plant_variety)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.plant_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.plantation_notes)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            plant.copy(
                                name = name.trim(),
                                variety = variety.trim(),
                                count = countText.toIntOrNull() ?: plant.count,
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
