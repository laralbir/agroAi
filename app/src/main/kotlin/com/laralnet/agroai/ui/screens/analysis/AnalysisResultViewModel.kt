package com.laralnet.agroai.ui.screens.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.infrastructure.gemma.PhotoAnalysisResult
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import com.laralnet.agroai.photoanalysis.application.handler.DeleteAnalysisHandler
import com.laralnet.agroai.photoanalysis.application.query.GetAnalysisQuery
import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisResultUiState(
    val record: AnalysisRecord? = null,
    val parsedResult: PhotoAnalysisResult? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteConfirm: Boolean = false
)

@HiltViewModel
class AnalysisResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnalysisQuery: GetAnalysisQuery,
    private val deleteAnalysisHandler: DeleteAnalysisHandler
) : ViewModel() {

    private val analysisId: String = checkNotNull(savedStateHandle["analysisId"])

    private val _uiState = MutableStateFlow(AnalysisResultUiState())
    val uiState: StateFlow<AnalysisResultUiState> = _uiState.asStateFlow()

    private val _scheduleEvent = Channel<ScheduleNavEvent>(Channel.BUFFERED)
    val scheduleEvent = _scheduleEvent.receiveAsFlow()

    private val _deletedEvent = Channel<Unit>(Channel.BUFFERED)
    val deletedEvent = _deletedEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val record = getAnalysisQuery(analysisId)
            if (record == null) {
                _uiState.update { it.copy(isLoading = false, error = "Analysis not found") }
                return@launch
            }
            val parsed = parsePhotoAnalysisResponse(record.rawResponse)
            _uiState.update { it.copy(isLoading = false, record = record, parsedResult = parsed) }
        }
    }

    fun scheduleSuggestion(plantationId: String, suggestion: TreatmentSuggestion, rawResponse: String) {
        viewModelScope.launch {
            _scheduleEvent.send(
                ScheduleNavEvent(
                    plantationId = plantationId,
                    suggestion = suggestion,
                    rawAnalysis = rawResponse.take(3000).ifBlank { null }
                )
            )
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
        viewModelScope.launch {
            deleteAnalysisHandler.handle(analysisId)
            _deletedEvent.send(Unit)
        }
    }
}
