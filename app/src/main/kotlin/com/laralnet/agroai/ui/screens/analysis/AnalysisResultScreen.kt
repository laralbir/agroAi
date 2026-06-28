package com.laralnet.agroai.ui.screens.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.R
import com.laralnet.agroai.ui.components.SimpleMarkdownText
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScheduleTreatment: ((plantationId: String, type: String, title: String, description: String, rawAnalysis: String?) -> Unit)? = null,
    viewModel: AnalysisResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate back after deletion
    LaunchedEffect(Unit) {
        viewModel.deletedEvent.collect { onNavigateBack() }
    }

    // Forward schedule events to caller
    LaunchedEffect(Unit) {
        viewModel.scheduleEvent.collect { event ->
            onNavigateToScheduleTreatment?.invoke(
                event.plantationId,
                event.suggestion.type,
                event.suggestion.title.ifBlank { event.suggestion.type },
                event.suggestion.description,
                event.rawAnalysis
            )
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(stringResource(R.string.analysis_delete_confirm_title)) },
            text = { Text(stringResource(R.string.analysis_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analysis_result_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && uiState.record != null) {
                        IconButton(onClick = { viewModel.requestDelete() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.analysis_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                val record = uiState.record!!
                val result = uiState.parsedResult

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Context header card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val dateStr = DateTimeFormatter
                                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                                .withZone(ZoneId.systemDefault())
                                .format(record.createdAt)
                            Text(
                                dateStr,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!record.plantationName.isNullOrBlank()) {
                                Text(
                                    record.plantationName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (!record.plantTypeName.isNullOrBlank()) {
                                Text(
                                    record.plantTypeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Full markdown analysis (strip JSON block)
                    val displayText = record.rawResponse
                        .substringBefore("```json").trimEnd()
                        .ifBlank { record.rawResponse }
                    if (displayText.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            SimpleMarkdownText(
                                text = displayText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Structured suggestion cards
                    if (result != null && result.suggestions.isNotEmpty()) {
                        Text(
                            stringResource(R.string.analysis_suggestions),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        result.suggestions.forEach { suggestion ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SuggestionCard(
                                    suggestion = suggestion,
                                    onSchedule = if (record.plantationId != null &&
                                        onNavigateToScheduleTreatment != null
                                    ) {
                                        {
                                            viewModel.scheduleSuggestion(
                                                record.plantationId,
                                                suggestion,
                                                record.rawResponse
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
