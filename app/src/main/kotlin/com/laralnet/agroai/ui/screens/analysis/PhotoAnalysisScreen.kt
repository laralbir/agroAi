package com.laralnet.agroai.ui.screens.analysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.ui.components.SimpleMarkdownText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAnalysisScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScheduleTreatment: ((plantationId: String, type: String, title: String, description: String, rawAnalysis: String?) -> Unit)? = null,
    viewModel: PhotoAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setImageUri(it) } }

    // Camera capture — TakePicture writes to a cached file via FileProvider
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let { viewModel.setImageUri(it) } }

    fun launchCamera() {
        val photoFile = File(context.cacheDir, "photos").also { it.mkdirs() }
            .let { File(it, "photo_${System.currentTimeMillis()}.jpg") }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analysis_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.modelLoaded) {
                NoModelWarning(modifier = Modifier.fillMaxSize().padding(32.dp))
                return@Column
            }

            // ---- Vision warning ----
            if (!uiState.supportsVision) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(R.string.analysis_vision_not_supported),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ---- Photo preview ----
            uiState.imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentScale = ContentScale.Crop
                )
            } ?: PhotoPlaceholder(modifier = Modifier.fillMaxWidth().height(240.dp))

            // ---- Camera / gallery buttons ----
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.analysis_choose_gallery))
                }
                Button(
                    onClick = { launchCamera() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.analysis_take_photo))
                }
            }

            // ---- Plantation & plant type selectors ----
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlantationDropdown(
                    plantations = uiState.plantations,
                    selectedId = uiState.selectedPlantationId,
                    onSelect = viewModel::selectPlantation
                )

                val selectedPlantation = uiState.plantations
                    .firstOrNull { it.id == uiState.selectedPlantationId }
                if (selectedPlantation != null && selectedPlantation.plants.isNotEmpty()) {
                    PlantTypeDropdown(
                        plants = selectedPlantation.plants,
                        selectedId = uiState.selectedPlantTypeId,
                        onSelect = viewModel::selectPlantType
                    )
                }

                if (uiState.selectedPlantationId == null && uiState.plantations.isNotEmpty()) {
                    Text(
                        stringResource(R.string.analysis_select_required_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ---- Optional user question ----
            OutlinedTextField(
                value = uiState.userQuestion,
                onValueChange = viewModel::setUserQuestion,
                label = { Text(stringResource(R.string.analysis_question_label)) },
                placeholder = { Text(stringResource(R.string.analysis_question_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                minLines = 2,
                maxLines = 4
            )

            // ---- Analyze button ----
            val canAnalyze = uiState.imageUri != null && !uiState.isAnalyzing &&
                (uiState.plantations.isEmpty() ||
                    (uiState.selectedPlantationId != null && uiState.selectedPlantTypeId != null))
            Button(
                onClick = { viewModel.analyzePhoto() },
                enabled = canAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.analysis_analyze_btn))
            }
            if (uiState.imageUri != null && !canAnalyze && !uiState.isAnalyzing &&
                uiState.plantations.isNotEmpty()
            ) {
                Text(
                    stringResource(R.string.analysis_btn_disabled_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ---- Progress ----
            if (uiState.isAnalyzing) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.analysis_analyzing))
                }
            }

            // ---- Analysis result ----
            uiState.analysisResult?.let { result ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Full markdown response (strips the JSON block visually)
                    val displayText = result.rawResponse
                        .substringBefore("```json").trimEnd()
                        .ifBlank { result.rawResponse }
                    if (displayText.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SimpleMarkdownText(
                                text = displayText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Structured action cards
                    if (result.suggestions.isNotEmpty()) {
                        Text(
                            stringResource(R.string.analysis_suggestions),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        result.suggestions.forEach { suggestion ->
                            SuggestionCard(
                                suggestion = suggestion,
                                onSchedule = if (viewModel.plantationId != null &&
                                    onNavigateToScheduleTreatment != null
                                ) {
                                    {
                                        onNavigateToScheduleTreatment.invoke(
                                            viewModel.plantationId!!,
                                            suggestion.type,
                                            suggestion.title.ifBlank { suggestion.type },
                                            suggestion.description,
                                            result.rawResponse.take(3000)
                                        )
                                    }
                                } else null
                            )
                        }
                    } else {
                        // Fallback: single schedule button when model didn't produce structured actions
                        val pid = viewModel.plantationId
                        if (pid != null && onNavigateToScheduleTreatment != null) {
                            OutlinedButton(
                                onClick = {
                                    onNavigateToScheduleTreatment.invoke(
                                        pid, "OTRO", "AI Analysis",
                                        result.rawResponse.take(400),
                                        result.rawResponse.take(3000)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.analysis_schedule_calendar))
                            }
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Suggestion card ----
@Composable
private fun SuggestionCard(
    suggestion: TreatmentSuggestion,
    onSchedule: (() -> Unit)?
) {
    val type = TreatmentType.entries.firstOrNull { it.name == suggestion.type } ?: TreatmentType.OTRO
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(type.emoji(), style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val titleText = suggestion.title.ifBlank {
                    type.name.lowercase().replaceFirstChar { it.uppercaseChar() }
                }
                Text(titleText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (suggestion.description.isNotBlank()) {
                    Text(
                        suggestion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    if (suggestion.urgency.isNotBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(suggestion.urgency, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    suggestion.suggestedDate?.let { date ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text("📅 $date", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (onSchedule != null) {
                    OutlinedButton(
                        onClick = onSchedule,
                        modifier = Modifier.padding(top = 2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.analysis_schedule_calendar),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

private fun TreatmentType.emoji() = when (this) {
    TreatmentType.RIEGO -> "💧"
    TreatmentType.PODA -> "✂️"
    TreatmentType.COSECHA -> "🌾"
    TreatmentType.FERTILIZACION -> "🧪"
    TreatmentType.FUMIGACION -> "🌫️"
    TreatmentType.INJERTO -> "🌿"
    TreatmentType.TRANSPLANTE -> "🪴"
    TreatmentType.OTRO -> "📋"
}

// ---- Plantation dropdown ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantationDropdown(
    plantations: List<com.laralnet.agroai.plantation.domain.model.Plantation>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    if (plantations.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val selected = plantations.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.analysis_select_plantation)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            plantations.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    onClick = { onSelect(p.id); expanded = false }
                )
            }
        }
    }
}

// ---- Plant type dropdown ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantTypeDropdown(
    plants: List<com.laralnet.agroai.plantation.domain.model.PlantType>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = plants.firstOrNull { it.id == selectedId }
    val selectedLabel = selected?.let {
        if (it.variety.isNotBlank()) "${it.name} · ${it.variety}" else it.name
    } ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.analysis_select_plant_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            plants.forEach { pt ->
                val label = if (pt.variety.isNotBlank()) "${pt.name} · ${pt.variety}" else pt.name
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(pt.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.analysis_select_photo),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoModelWarning(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.analysis_no_model),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
