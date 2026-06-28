package com.laralnet.agroai.ui.screens.plantation.wizard

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.plantation.domain.model.PlantationType

private fun PlantationType.resolveLabel(context: Context): String {
    val resId = context.resources.getIdentifier(labelResKey, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantationWizardScreen(
    onNavigateBack: () -> Unit,
    onPlantationSaved: (String) -> Unit,
    onOpenMapPicker: () -> Unit,
    viewModel: PlantationWizardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()

    val steps = listOf(
        stringResource(R.string.plantation_wizard_step1),
        stringResource(R.string.plantation_wizard_step2),
        stringResource(R.string.plantation_wizard_step3),
        stringResource(R.string.plantation_wizard_step4)
    )

    LaunchedEffect(uiState.savedId) {
        uiState.savedId?.let { onPlantationSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) stringResource(R.string.plantation_edit)
                        else stringResource(R.string.plantation_add)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 0) onNavigateBack()
                        else viewModel.previousStep()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step indicator
            StepIndicator(currentStep = currentStep, steps = steps)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (currentStep) {
                    0 -> Step1BasicInfo(state = uiState, viewModel = viewModel)
                    1 -> Step2Location(state = uiState, viewModel = viewModel, onOpenMapPicker = onOpenMapPicker)
                    2 -> Step3Plants(state = uiState, viewModel = viewModel)
                    3 -> Step4Summary(state = uiState)
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = viewModel::previousStep) {
                        Text(stringResource(R.string.btn_back))
                    }
                }
                Button(
                    onClick = {
                        if (currentStep < steps.size - 1) viewModel.nextStep()
                        else viewModel.save()
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (currentStep < steps.size - 1)
                                stringResource(R.string.btn_next)
                            else
                                stringResource(R.string.btn_save)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, steps: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / steps.size.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${currentStep + 1}/${steps.size} — ${steps[currentStep]}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Step1BasicInfo(state: PlantationWizardState, viewModel: PlantationWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(R.string.plantation_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.areaSqMeters,
            onValueChange = viewModel::setArea,
            label = { Text(stringResource(R.string.plantation_area)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text("m²") }
        )

        Text(stringResource(R.string.plantation_type), style = MaterialTheme.typography.labelLarge)

        PlantationTypeGrid(selected = state.type, onSelected = viewModel::setType)

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::setNotes,
            label = { Text(stringResource(R.string.plantation_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantationTypeGrid(
    selected: PlantationType?,
    onSelected: (PlantationType) -> Unit
) {
    val context = LocalContext.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2
    ) {
        PlantationType.entries.forEach { type ->
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text("${type.defaultIconEmoji} ${type.resolveLabel(context)}") }
            )
        }
    }
}

@Composable
private fun Step2Location(
    state: PlantationWizardState,
    viewModel: PlantationWizardViewModel,
    onOpenMapPicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Location summary card — shown when location is already set
        val hasLocation = state.latitude != null && state.longitude != null
        if (hasLocation || state.municipality.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.location_selected), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    if (state.municipality.isNotBlank()) {
                        Text(
                            buildString {
                                append(state.municipality)
                                if (state.province.isNotBlank()) append(", ${state.province}")
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (state.latitude != null) {
                        Text(
                            "%.5f, %.5f".format(state.latitude, state.longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Map picker button — primary action
        Button(
            onClick = onOpenMapPicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (hasLocation) stringResource(R.string.wizard_change_map) else stringResource(R.string.wizard_pick_map))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            stringResource(R.string.wizard_or_enter_manually),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = state.municipality,
            onValueChange = viewModel::setMunicipality,
            label = { Text(stringResource(R.string.wizard_municipality)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.province,
            onValueChange = viewModel::setProvince,
            label = { Text(stringResource(R.string.wizard_province)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun Step3Plants(state: PlantationWizardState, viewModel: PlantationWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(
            onClick = viewModel::addPlant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.plantation_add_plant))
        }
        state.plantForms.forEachIndexed { index, plant ->
            PlantFormCard(
                plant = plant,
                onUpdate = { updated -> viewModel.updatePlant(index, updated) },
                onRemove = { viewModel.removePlant(index) }
            )
        }
    }
}

@Composable
private fun PlantFormCard(
    plant: PlantForm,
    onUpdate: (PlantForm) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.wizard_plant_title), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onRemove) { Text(stringResource(R.string.btn_delete)) }
            }
            OutlinedTextField(
                value = plant.name,
                onValueChange = { onUpdate(plant.copy(name = it)) },
                label = { Text(stringResource(R.string.plant_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = plant.variety,
                onValueChange = { onUpdate(plant.copy(variety = it)) },
                label = { Text(stringResource(R.string.plant_variety)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = plant.count,
                onValueChange = { new ->
                    val digits = new.filter { it.isDigit() }
                    val cleaned = digits.trimStart('0').let { if (it.isEmpty() && digits.isNotEmpty()) "0" else it }
                    if (cleaned.isEmpty() || (cleaned.toIntOrNull() ?: 0) > 0) {
                        onUpdate(plant.copy(count = cleaned))
                    }
                },
                label = { Text(stringResource(R.string.plant_count)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun Step4Summary(state: PlantationWizardState) {
    val context = LocalContext.current
    val typeLabel = state.type?.resolveLabel(context) ?: "-"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.plantation_wizard_step4), style = MaterialTheme.typography.titleLarge)
        SummaryRow(stringResource(R.string.wizard_summary_name), state.name)
        SummaryRow(stringResource(R.string.wizard_summary_type), typeLabel)
        SummaryRow(stringResource(R.string.wizard_summary_area), "${state.areaSqMeters} m²")
        SummaryRow(stringResource(R.string.plantation_location), state.address.ifBlank { state.municipality })
        SummaryRow(stringResource(R.string.plantation_plants), stringResource(R.string.wizard_plants_count, state.plantForms.size))
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider()
}
