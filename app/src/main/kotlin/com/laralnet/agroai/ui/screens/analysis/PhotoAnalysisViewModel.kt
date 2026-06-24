package com.laralnet.agroai.ui.screens.analysis

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.gemma.GemmaInferenceEngine
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoAnalysisUiState(
    val imageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val streamingText: String = "",
    val suggestions: List<TreatmentSuggestion> = emptyList(),
    val rawResponse: String = "",
    val error: String? = null,
    val modelLoaded: Boolean = false
)

@HiltViewModel
class PhotoAnalysisViewModel @Inject constructor(
    private val gemmaEngine: GemmaInferenceEngine,
    private val modelRepository: AIModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoAnalysisUiState())
    val uiState: StateFlow<PhotoAnalysisUiState> = _uiState.asStateFlow()

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
        _uiState.update { it.copy(imageUri = uri, isAnalyzing = true, suggestions = emptyList(), error = null, streamingText = "") }

        val prompt = modelRepository.findPromptTemplate("photo_analysis")
            ?: PromptTemplate.photoAnalysisDefault()

        val responseBuilder = StringBuilder()

        gemmaEngine.analyzePhoto(uri, prompt.systemContext, prompt.content)
            .collect { chunk ->
                responseBuilder.append(chunk)
                _uiState.update { it.copy(streamingText = responseBuilder.toString()) }
            }

        val fullResponse = responseBuilder.toString()
        val suggestions = parseSuggestions(fullResponse)

        _uiState.update { state ->
            state.copy(
                isAnalyzing = false,
                rawResponse = fullResponse,
                suggestions = suggestions,
                streamingText = ""
            )
        }
    }

    fun scheduleSuggestion(suggestion: TreatmentSuggestion) {
        // Navigate to schedule dialog — handled in UI layer
    }

    private fun parseSuggestions(response: String): List<TreatmentSuggestion> {
        // Attempt JSON parse; fallback to single suggestion from raw text
        return try {
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                // Simple parsing — in production use Gson/kotlinx.serialization
                listOf(
                    TreatmentSuggestion(
                        type = "Analysis result",
                        description = response,
                        urgency = "review",
                        suggestedDate = null
                    )
                )
            } else {
                listOf(
                    TreatmentSuggestion(
                        type = "Analysis result",
                        description = response,
                        urgency = "review",
                        suggestedDate = null
                    )
                )
            }
        } catch (_: Exception) {
            listOf(
                TreatmentSuggestion(
                    type = "Analysis",
                    description = response,
                    urgency = "review",
                    suggestedDate = null
                )
            )
        }
    }
}
