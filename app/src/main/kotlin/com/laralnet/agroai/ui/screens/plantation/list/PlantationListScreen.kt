package com.laralnet.agroai.ui.screens.plantation.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.ui.components.AppTopBar
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.ui.screens.home.HomeViewModel
import com.laralnet.agroai.ui.screens.home.toEmoji
import com.laralnet.agroai.weather.domain.model.WeatherData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantationListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToWizard: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val plantations by viewModel.plantations.collectAsState()
    val weatherByPlantation by viewModel.weatherByPlantation.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.plantation_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToWizard) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.plantation_add))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = 8.dp, bottom = 80.dp, start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(plantations, key = { it.id }) { plantation ->
                PlantationListItem(
                    plantation = plantation,
                    weather = weatherByPlantation[plantation.id],
                    onClick = { onNavigateToDetail(plantation.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantationListItem(
    plantation: Plantation,
    weather: WeatherData?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(plantation.name, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Text(
                    "${plantation.type.defaultIconEmoji} ${plantation.areaSqMeters.toInt()} m²" +
                            if (plantation.location.municipality.isNotBlank()) " • ${plantation.location.municipality}" else ""
                )
            },
            leadingContent = {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(plantation.type.defaultIconEmoji, style = MaterialTheme.typography.titleLarge)
                    }
                }
            },
            trailingContent = {
                val current = weather?.current
                if (current != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            current.condition.toEmoji(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "%.0f°".format(
                                weather.forecast.firstOrNull()?.maxTempCelsius
                                    ?: current.temperatureCelsius
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
    }
}
