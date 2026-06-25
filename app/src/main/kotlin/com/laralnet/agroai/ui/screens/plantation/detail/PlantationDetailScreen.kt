package com.laralnet.agroai.ui.screens.plantation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.treatment.domain.model.Treatment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantationDetailScreen(
    plantationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    viewModel: PlantationDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(plantationId) { viewModel.load(plantationId) }

    val plantation by viewModel.plantation.collectAsState()
    val treatments by viewModel.treatments.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plantation?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* edit */ }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.btn_save))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAnalysis) {
                Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.analysis_take_photo))
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
                item { PlantationInfoSection(plantation = p) }
                item {
                    Text(
                        stringResource(R.string.plantation_detail_treatments),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
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
                        TreatmentCard(treatment = treatment)
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun PlantationInfoSection(plantation: Plantation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text(plantation.type.defaultIconEmoji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(plantation.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${plantation.areaSqMeters.toInt()} m²",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (plantation.location.displayAddress.isNotBlank()) {
                Text(plantation.location.displayAddress, style = MaterialTheme.typography.bodyMedium)
            }
            if (plantation.plants.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.plantation_plants), style = MaterialTheme.typography.labelLarge)
                val unitsLabel = stringResource(R.string.plantation_units)
                plantation.plants.forEach { plant ->
                    Text(
                        text = buildString {
                            append("• ${plant.name}")
                            if (plant.variety.isNotBlank()) append(" (${plant.variety})")
                            if (plant.count > 0) append(" — ${plant.count} $unitsLabel")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TreatmentCard(treatment: Treatment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(treatment.title) },
            supportingContent = {
                Text(
                    "${treatment.type.name} • ${treatment.status.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
    }
}
