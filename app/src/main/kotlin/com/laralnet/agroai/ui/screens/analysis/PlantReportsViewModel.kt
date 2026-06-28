package com.laralnet.agroai.ui.screens.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.photoanalysis.application.handler.DeleteAnalysisHandler
import com.laralnet.agroai.photoanalysis.application.query.ObserveAnalysesQuery
import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantReportsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAnalysesQuery: ObserveAnalysesQuery,
    private val deleteAnalysisHandler: DeleteAnalysisHandler
) : ViewModel() {

    val plantationId: String = checkNotNull(savedStateHandle["plantationId"])
    val plantTypeId: String = checkNotNull(savedStateHandle["plantTypeId"])

    val analyses: StateFlow<List<AnalysisRecord>> =
        observeAnalysesQuery(plantationId)
            .map { list -> list.filter { it.plantTypeId == plantTypeId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteAnalysis(id: String) {
        viewModelScope.launch { deleteAnalysisHandler.handle(id) }
    }
}
