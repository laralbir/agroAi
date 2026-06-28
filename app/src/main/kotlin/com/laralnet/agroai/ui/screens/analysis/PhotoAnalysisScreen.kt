package com.laralnet.agroai.ui.screens.analysis

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.laralnet.agroai.R
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
    ) { uri: Uri? -> uri?.let { viewModel.analyzePhoto(it) } }

    // Camera capture — TakePicture writes to a cached file and returns Boolean success
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let { viewModel.analyzePhoto(it) } }

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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.modelLoaded) {
                NoModelWarning(modifier = Modifier.fillMaxSize().padding(32.dp))
                return@Column
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
                onClick = { uiState.imageUri?.let { viewModel.analyzePhoto(it) } },
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

            // ---- Progress / streaming ----
            if (uiState.isAnalyzing) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.analysis_analyzing))
                    if (uiState.streamingText.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SimpleMarkdownText(
                                text = uiState.streamingText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            // ---- Analysis result ----
            uiState.analysisResult?.let { result ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (result.rawResponse.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SimpleMarkdownText(
                                text = result.rawResponse,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    val pid = viewModel.plantationId
                    if (pid != null && onNavigateToScheduleTreatment != null) {
                        Button(
                            onClick = {
                                onNavigateToScheduleTreatment.invoke(
                                    pid,
                                    "OTRO",
                                    "AI Analysis",
                                    result.rawResponse.take(400),
                                    result.rawResponse.take(3000)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.analysis_schedule_calendar))
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

// ---- Supporting composables (unchanged from before, except SuggestionCard date string) ----
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
