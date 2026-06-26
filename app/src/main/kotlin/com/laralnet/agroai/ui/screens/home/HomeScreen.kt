package com.laralnet.agroai.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
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
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.ui.screens.treatment.resolveLabel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
    val upcomingTreatments by viewModel.upcomingTreatments.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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

                // Upcoming treatments (limited to 3)
                val preview = upcomingTreatments.take(3)
                if (preview.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.home_upcoming_treatments),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(preview, key = { "t_${it.id}" }) { treatment ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingTreatmentCard(treatment: Treatment, onClick: () -> Unit) {
    val context = LocalContext.current
    val statusColor = when (treatment.status) {
        TreatmentStatus.PENDING -> MaterialTheme.colorScheme.secondary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantationSummaryCard(
    plantation: Plantation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
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
                Text(
                    text = plantation.name,
                    style = MaterialTheme.typography.titleMedium
                )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
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
