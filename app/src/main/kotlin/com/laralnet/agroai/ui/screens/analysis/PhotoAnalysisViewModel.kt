package com.laralnet.agroai.ui.screens.analysis

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.gemma.GemmaInferenceEngine
import com.laralnet.agroai.aimodel.infrastructure.gemma.PhotoAnalysisResult
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import com.laralnet.agroai.photoanalysis.application.handler.SaveAnalysisCommand
import com.laralnet.agroai.photoanalysis.application.handler.SaveAnalysisHandler
import com.laralnet.agroai.photoanalysis.application.query.ObserveAnalysesQuery
import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.weather.application.handler.RefreshWeatherHandler
import com.laralnet.agroai.weather.application.query.ObserveWeatherQuery
import com.laralnet.agroai.weather.domain.model.WeatherData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PhotoAnalysisUiState(
    val imageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val streamingText: String = "",
    val analysisResult: PhotoAnalysisResult? = null,
    val rawResponse: String = "",
    val error: String? = null,
    val modelLoaded: Boolean = false,
    val supportsVision: Boolean = false,
    val plantations: List<Plantation> = emptyList(),
    val selectedPlantationId: String? = null,
    val selectedPlantTypeId: String? = null,
    val userQuestion: String = "",
    val recentAnalyses: List<AnalysisRecord> = emptyList()
)

data class ScheduleNavEvent(
    val plantationId: String,
    val suggestion: TreatmentSuggestion,
    val rawAnalysis: String?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PhotoAnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val gemmaEngine: GemmaInferenceEngine,
    private val modelRepository: AIModelRepository,
    private val plantationRepository: PlantationRepository,
    private val observeWeatherQuery: ObserveWeatherQuery,
    private val refreshWeatherHandler: RefreshWeatherHandler,
    private val saveAnalysisHandler: SaveAnalysisHandler,
    private val observeAnalysesQuery: ObserveAnalysesQuery
) : ViewModel() {

    // Pre-selected from nav args (e.g. from PlantCard "Analyze" button)
    private val navPlantationId: String? = savedStateHandle["plantationId"]
    private val navPlantTypeId: String? = savedStateHandle["plantTypeId"]

    private val _selectedPlantationId = MutableStateFlow(navPlantationId)
    private val _selectedPlantTypeId = MutableStateFlow(navPlantTypeId)
    private val _userQuestion = MutableStateFlow("")

    private val _uiState = MutableStateFlow(
        PhotoAnalysisUiState(
            selectedPlantationId = navPlantationId,
            selectedPlantTypeId = navPlantTypeId
        )
    )
    val uiState: StateFlow<PhotoAnalysisUiState> = _uiState.asStateFlow()

    private val _scheduleEvent = Channel<ScheduleNavEvent>(Channel.BUFFERED)
    val scheduleEvent = _scheduleEvent.receiveAsFlow()

    private val _navigateToResult = Channel<String>(Channel.BUFFERED)
    val navigateToResult = _navigateToResult.receiveAsFlow()

    private var analysisJob: Job? = null

    // All plantations — for the dropdown
    private val plantations: StateFlow<List<Plantation>> = plantationRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The currently selected plantation (derived from list + selectedId)
    private val selectedPlantation: StateFlow<Plantation?> = combine(
        plantations, _selectedPlantationId
    ) { list, id -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Weather for selected plantation
    val currentWeather: StateFlow<WeatherData?> = selectedPlantation
        .flatMapLatest { p ->
            if (p != null && p.location.hasCoordinates)
                observeWeatherQuery(p.location.latitude!!, p.location.longitude!!)
            else
                flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val plantationId: String? get() = _selectedPlantationId.value

    init {
        checkModelLoaded()

        // Push plantation list into uiState
        viewModelScope.launch {
            plantations.collect { list ->
                _uiState.update { it.copy(plantations = list) }
            }
        }

        // Push selection fields into uiState
        viewModelScope.launch {
            combine(_selectedPlantationId, _selectedPlantTypeId, _userQuestion) { pId, ptId, q ->
                Triple(pId, ptId, q)
            }.collect { (pId, ptId, q) ->
                _uiState.update { it.copy(selectedPlantationId = pId, selectedPlantTypeId = ptId, userQuestion = q) }
            }
        }

        // Observe recent analyses filtered by selected plantation
        viewModelScope.launch {
            _selectedPlantationId.flatMapLatest { pid ->
                observeAnalysesQuery(pid)
            }.collect { records ->
                _uiState.update { it.copy(recentAnalyses = records) }
            }
        }

        // Refresh weather when selection changes and has coordinates
        viewModelScope.launch {
            selectedPlantation.filterNotNull().collect { p ->
                if (p.location.hasCoordinates)
                    refreshWeatherHandler.handle(p.location.latitude!!, p.location.longitude!!)
            }
        }
    }

    private fun checkModelLoaded() = viewModelScope.launch {
        val activeModel = modelRepository.findActive()
        val loaded = activeModel?.filePath != null && gemmaEngine.isModelLoaded()
        if (!loaded && activeModel?.filePath != null) {
            runCatching { gemmaEngine.loadModel(activeModel.filePath!!) }
                .onSuccess {
                    _uiState.update {
                        it.copy(modelLoaded = true, supportsVision = gemmaEngine.supportsImageAnalysis())
                    }
                }
                .onFailure { _uiState.update { it.copy(modelLoaded = false, supportsVision = false) } }
        } else {
            _uiState.update {
                it.copy(
                    modelLoaded = loaded,
                    supportsVision = if (loaded) gemmaEngine.supportsImageAnalysis() else false
                )
            }
        }
    }

    fun selectPlantation(id: String) {
        _selectedPlantationId.value = id
        _selectedPlantTypeId.value = null
    }

    fun selectPlantType(id: String) {
        _selectedPlantTypeId.value = id
    }

    fun setUserQuestion(text: String) {
        _userQuestion.value = text
    }

    /** Store the URI from camera/gallery without triggering analysis. */
    fun setImageUri(uri: Uri) {
        _uiState.update {
            it.copy(imageUri = uri, analysisResult = null, error = null, streamingText = "", rawResponse = "")
        }
    }

    /** Cancel an in-progress analysis. */
    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        _uiState.update { it.copy(isAnalyzing = false, streamingText = "", error = null) }
    }

    /** Start analysis using the stored imageUri. Must be called from the Analyze button. */
    fun analyzePhoto() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            val uri = _uiState.value.imageUri ?: return@launch
            _uiState.update {
                it.copy(isAnalyzing = true, analysisResult = null, error = null, streamingText = "")
            }

            val plantation = selectedPlantation.value
            val plantType = plantation?.plants?.firstOrNull { it.id == _selectedPlantTypeId.value }
            val weather = currentWeather.value
            val lang = resolveLanguage()

            val baseTemplate = modelRepository.findPromptTemplate("photo_analysis")
                ?: PromptTemplate.photoAnalysisDefault()

            val enrichedPrompt = buildEnrichedPrompt(
                baseTemplate = baseTemplate.content,
                plantation = plantation,
                plantType = plantType,
                weather = weather,
                userQuestion = _userQuestion.value.trim(),
                language = lang
            )

            val responseBuilder = StringBuilder()

            val collectResult = runCatching {
                gemmaEngine.analyzePhoto(uri, baseTemplate.systemContext, enrichedPrompt)
                    .collect { chunk ->
                        responseBuilder.append(chunk)
                        _uiState.update { it.copy(streamingText = responseBuilder.toString()) }
                    }
            }

            if (collectResult.exceptionOrNull() is CancellationException) {
                throw collectResult.exceptionOrNull()!!
            }

            collectResult.onFailure { e ->
                _uiState.update { it.copy(isAnalyzing = false, error = e.message, streamingText = "") }
                return@launch
            }

            val fullResponse = responseBuilder.toString()
            val result = parsePhotoAnalysisResponse(fullResponse)

            // Persist the analysis record
            val saveResult = saveAnalysisHandler.handle(
                SaveAnalysisCommand(
                    plantationId = plantation?.id,
                    plantTypeId = plantType?.id,
                    plantationName = plantation?.name,
                    plantTypeName = plantType?.let { pt ->
                        if (pt.variety.isNotBlank()) "${pt.name} · ${pt.variety}" else pt.name
                    },
                    rawResponse = fullResponse
                )
            )

            _uiState.update { s ->
                s.copy(
                    isAnalyzing = false,
                    rawResponse = fullResponse,
                    analysisResult = result,
                    streamingText = ""
                )
            }

            // Navigate to result screen
            saveResult.onSuccess { record ->
                _navigateToResult.send(record.id)
            }
        }
    }

    fun scheduleSuggestion(suggestion: TreatmentSuggestion) {
        val pid = plantationId ?: return
        viewModelScope.launch {
            _scheduleEvent.send(
                ScheduleNavEvent(
                    plantationId = pid,
                    suggestion = suggestion,
                    rawAnalysis = _uiState.value.rawResponse.ifBlank { null }
                )
            )
        }
    }

    // ---- helpers ----

    private fun buildEnrichedPrompt(
        baseTemplate: String,
        plantation: Plantation?,
        plantType: PlantType?,
        weather: WeatherData?,
        userQuestion: String,
        language: String
    ): String = buildString {
        append(baseTemplate)
        append("\n\nToday's date: ${LocalDate.now(ZoneId.systemDefault())}. All suggestedDate values in the JSON block must use this year or later.\n")
        append("Respond in $language.\n")

        if (plantation != null || plantType != null) {
            append("\n### Context\n")
            plantation?.let { p ->
                append("Plantation type: ${p.type.name} (${p.type.defaultIconEmoji})\n")
                val loc = p.location
                if (loc.municipality.isNotBlank())
                    append("Location: ${loc.municipality}, ${loc.province}\n")
                else if (loc.displayAddress.isNotBlank())
                    append("Location: ${loc.displayAddress}\n")
                if (p.notes.isNotBlank()) append("Plantation notes: ${p.notes}\n")
            }
            plantType?.let { pt ->
                append("Plant: ${pt.name}")
                if (pt.variety.isNotBlank()) append(" (${pt.variety})")
                if (pt.count > 0) append(", ${pt.count} units")
                append("\n")
                if (pt.notes.isNotBlank()) append("Plant notes: ${pt.notes}\n")
            }
        }

        weather?.let { w ->
            append("\n### Current weather\n")
            w.current?.let { c ->
                append(
                    "${c.condition.name.replace('_', ' ')}, ${c.temperatureCelsius}°C " +
                        "(feels like ${c.feelsLikeCelsius}°C), humidity ${c.humidity}%, " +
                        "wind ${c.windSpeedKmh} km/h, precipitation ${c.precipitationMm} mm\n"
                )
            }
            if (w.forecast.isNotEmpty()) {
                append("\n### 15-day forecast\n")
                val fmt = DateTimeFormatter.ofPattern("MMM dd")
                w.forecast.forEach { day ->
                    val date = LocalDate.ofInstant(day.date, ZoneId.systemDefault()).format(fmt)
                    append(
                        "$date: ${day.condition.name.replace('_', ' ')} " +
                            "${day.maxTempCelsius.toInt()}/${day.minTempCelsius.toInt()}°C, " +
                            "precip ${day.precipitationProbability}%\n"
                    )
                }
            }
        }

        if (userQuestion.isNotBlank()) {
            append("\n### User question\n")
            append(userQuestion)
            append("\n")
        }
    }

    private fun resolveLanguage(): String {
        val langMode = context.getSharedPreferences("locale_pref", Context.MODE_PRIVATE)
            .getString("language_mode", "SYSTEM") ?: "SYSTEM"
        return when (langMode) {
            "ENGLISH" -> "English"
            "SPANISH" -> "Spanish"
            else -> if (context.resources.configuration.locales[0].language == "es") "Spanish" else "English"
        }
    }
}
