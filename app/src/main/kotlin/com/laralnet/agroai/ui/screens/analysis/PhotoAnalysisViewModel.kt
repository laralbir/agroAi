package com.laralnet.agroai.ui.screens.analysis

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.gemma.GemmaInferenceEngine
import com.laralnet.agroai.aimodel.infrastructure.gemma.PhotoAnalysisResult
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoAnalysisUiState(
    val imageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val streamingText: String = "",
    val analysisResult: PhotoAnalysisResult? = null,
    val rawResponse: String = "",
    val error: String? = null,
    val modelLoaded: Boolean = false
)

data class ScheduleNavEvent(
    val plantationId: String,
    val suggestion: TreatmentSuggestion,
    val rawAnalysis: String?
)

@HiltViewModel
class PhotoAnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gemmaEngine: GemmaInferenceEngine,
    private val modelRepository: AIModelRepository
) : ViewModel() {

    val plantationId: String? = savedStateHandle["plantationId"]

    private val _uiState = MutableStateFlow(PhotoAnalysisUiState())
    val uiState: StateFlow<PhotoAnalysisUiState> = _uiState.asStateFlow()

    private val _scheduleEvent = Channel<ScheduleNavEvent>(Channel.BUFFERED)
    val scheduleEvent = _scheduleEvent.receiveAsFlow()

    init {
        checkModelLoaded()
    }

    private fun checkModelLoaded() = viewModelScope.launch {
        val activeModel = modelRepository.findActive()
        val loaded = activeModel?.filePath != null && gemmaEngine.isModelLoaded()
        if (!loaded && activeModel?.filePath != null) {
            runCatching { gemmaEngine.loadModel(activeModel.filePath!!) }
                .onSuccess { _uiState.update { it.copy(modelLoaded = true) } }
                .onFailure { _uiState.update { it.copy(modelLoaded = false) } }
        } else {
            _uiState.update { it.copy(modelLoaded = loaded) }
        }
    }

    fun analyzePhoto(uri: Uri) = viewModelScope.launch {
        _uiState.update {
            it.copy(imageUri = uri, isAnalyzing = true, analysisResult = null, error = null, streamingText = "")
        }

        val prompt = modelRepository.findPromptTemplate("photo_analysis")
            ?: PromptTemplate.photoAnalysisDefault()

        val responseBuilder = StringBuilder()

        runCatching {
            gemmaEngine.analyzePhoto(uri, prompt.systemContext, prompt.content)
                .collect { chunk ->
                    responseBuilder.append(chunk)
                    _uiState.update { it.copy(streamingText = responseBuilder.toString()) }
                }
        }.onFailure { e ->
            _uiState.update { it.copy(isAnalyzing = false, error = e.message, streamingText = "") }
            return@launch
        }

        val fullResponse = responseBuilder.toString()
        val result = parsePhotoAnalysisResponse(fullResponse)

        _uiState.update { state ->
            state.copy(
                isAnalyzing = false,
                rawResponse = fullResponse,
                analysisResult = result,
                streamingText = ""
            )
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
}
