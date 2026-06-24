package com.laralnet.agroai.ui.screens.plantation.wizard

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
import com.laralnet.agroai.plantation.domain.model.PlantationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantationWizardScreen(
    onNavigateBack: () -> Unit,
    onPlantationCreated: (String) -> Unit,
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

    LaunchedEffect(uiState.createdId) {
        uiState.createdId?.let { onPlantationCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plantation_add)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 0) onNavigateBack()
                        else viewModel.previousStep()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (currentStep) {
                    0 -> Step1BasicInfo(state = uiState, viewModel = viewModel)
                    1 -> Step2Location(state = uiState, viewModel = viewModel)
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
                        else viewModel.createPlantation()
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlantationType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text("${type.defaultIconEmoji} ${type.labelResKey}") }
            )
        }
    }
}

@Composable
private fun Step2Location(state: PlantationWizardState, viewModel: PlantationWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.address,
            onValueChange = viewModel::setAddress,
            label = { Text(stringResource(R.string.plantation_location)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.municipality,
            onValueChange = viewModel::setMunicipality,
            label = { Text("Municipality") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.province,
            onValueChange = viewModel::setProvince,
            label = { Text("Province") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.municipalityCode,
            onValueChange = viewModel::setMunicipalityCode,
            label = { Text("AEMET municipality code (INE)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Required for weather data") }
        )
    }
}

@Composable
private fun Step3Plants(state: PlantationWizardState, viewModel: PlantationWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        state.plantForms.forEachIndexed { index, plant ->
            PlantFormCard(
                plant = plant,
                onUpdate = { updated -> viewModel.updatePlant(index, updated) },
                onRemove = { viewModel.removePlant(index) }
            )
        }
        OutlinedButton(
            onClick = viewModel::addPlant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.plantation_add_plant))
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
                Text("Plant", style = MaterialTheme.typography.titleSmall)
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
                onValueChange = { onUpdate(plant.copy(count = it)) },
                label = { Text(stringResource(R.string.plant_count)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun Step4Summary(state: PlantationWizardState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Summary", style = MaterialTheme.typography.titleLarge)
        SummaryRow("Name", state.name)
        SummaryRow("Type", state.type?.name ?: "-")
        SummaryRow("Area", "${state.areaSqMeters} m²")
        SummaryRow("Location", state.address.ifBlank { state.municipality })
        SummaryRow("Plants", "${state.plantForms.size} types")
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
