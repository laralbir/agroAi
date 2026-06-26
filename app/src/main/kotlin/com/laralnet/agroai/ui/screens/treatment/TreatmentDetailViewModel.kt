package com.laralnet.agroai.ui.screens.treatment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.treatment.application.command.CompleteTreatmentCommand
import com.laralnet.agroai.treatment.application.command.DeleteTreatmentCommand
import com.laralnet.agroai.treatment.application.handler.CompleteTreatmentHandler
import com.laralnet.agroai.treatment.application.handler.DeleteTreatmentHandler
import com.laralnet.agroai.treatment.application.query.GetTreatmentQuery
import com.laralnet.agroai.treatment.domain.model.Treatment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreatmentDetailState(
    val treatment: Treatment? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class TreatmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTreatmentQuery: GetTreatmentQuery,
    private val completeTreatmentHandler: CompleteTreatmentHandler,
    private val deleteTreatmentHandler: DeleteTreatmentHandler
) : ViewModel() {

    private val treatmentId: String = savedStateHandle["treatmentId"] ?: ""

    private val _state = MutableStateFlow(TreatmentDetailState())
    val state: StateFlow<TreatmentDetailState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        _state.update { it.copy(treatment = getTreatmentQuery(treatmentId), isLoading = false) }
    }

    fun complete(notes: String = "") = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        completeTreatmentHandler.handle(CompleteTreatmentCommand(treatmentId = treatmentId, notes = notes))
            .onSuccess { _state.update { it.copy(isLoading = false, isDone = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun delete() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        deleteTreatmentHandler.handle(DeleteTreatmentCommand(treatmentId = treatmentId))
            .onSuccess { _state.update { it.copy(isLoading = false, isDeleted = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }
}
