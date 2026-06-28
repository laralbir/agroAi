package com.laralnet.agroai.ui.screens.analysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.laralnet.agroai.R
import com.laralnet.agroai.ui.components.AppTopBar
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAnalysisScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScheduleTreatment: ((plantationId: String, type: String, title: String, description: String, rawAnalysis: String?) -> Unit)? = null,
    onNavigateToResult: ((analysisId: String) -> Unit)? = null,
    viewModel: PhotoAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Navigate to result screen when analysis completes and is saved
    LaunchedEffect(Unit) {
        viewModel.navigateToResult.collect { id ->
            onNavigateToResult?.invoke(id)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setImageUri(it) } }

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

    // Analysis-in-progress modal
    if (uiState.isAnalyzing) {
        AnalysisProgressDialog(
            wordCount = uiState.streamingText.split(Regex("\\s+")).count { it.isNotBlank() },
            onCancel = { viewModel.cancelAnalysis() }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
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

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // ---- Recent analyses history ----
            if (uiState.recentAnalyses.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                Text(
                    stringResource(R.string.analysis_history_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
                uiState.recentAnalyses.forEach { record ->
                    val dateStr = DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.SHORT)
                        .withZone(ZoneId.systemDefault())
                        .format(record.createdAt)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onNavigateToResult?.invoke(record.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!record.plantationName.isNullOrBlank()) {
                                Text(
                                    buildString {
                                        append(record.plantationName)
                                        if (!record.plantTypeName.isNullOrBlank()) {
                                            append(" · ")
                                            append(record.plantTypeName)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val preview = record.rawResponse
                                .substringBefore("```json").trimEnd()
                                .take(120)
                                .replace('\n', ' ')
                            if (preview.isNotBlank()) {
                                Text(
                                    preview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Analysis progress modal ----
@Composable
private fun AnalysisProgressDialog(
    wordCount: Int,
    onCancel: () -> Unit
) {
    val (stageRes, progressFraction) = analysisStage(wordCount)
    val percent = (progressFraction * 100).toInt()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()

                Text(
                    text = stringResource(R.string.analysis_analyzing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Stage description
                Text(
                    text = stringResource(stageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Linear progress bar + percentage
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.analysis_cancel))
                }
            }
        }
    }
}

private fun analysisStage(wordCount: Int): Pair<Int, Float> {
    return when {
        wordCount == 0 -> Pair(R.string.analysis_stage_starting, 0.05f)
        wordCount < 20 -> Pair(R.string.analysis_stage_analyzing_image, 0.05f + (wordCount / 20f) * 0.20f)
        wordCount < 60 -> Pair(R.string.analysis_stage_diagnosing, 0.25f + ((wordCount - 20f) / 40f) * 0.25f)
        wordCount < 120 -> Pair(R.string.analysis_stage_treatments, 0.50f + ((wordCount - 60f) / 60f) * 0.25f)
        else -> Pair(R.string.analysis_stage_recommendations, minOf(0.75f + ((wordCount - 120f) / 120f) * 0.15f, 0.90f))
    }
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
