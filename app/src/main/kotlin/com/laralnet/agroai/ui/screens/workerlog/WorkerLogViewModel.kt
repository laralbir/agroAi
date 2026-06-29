package com.laralnet.agroai.ui.screens.workerlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.domain.model.WorkerRun
import com.laralnet.agroai.aimodel.domain.repository.WorkerRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkerLogUiState(
    val runs: List<WorkerRun> = emptyList(),
    val isLoading: Boolean = true
)

data class WorkerRunDetailUiState(
    val run: WorkerRun? = null
)

@HiltViewModel
class WorkerLogViewModel @Inject constructor(
    private val workerRunRepository: WorkerRunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerLogUiState(isLoading = true))
    val uiState: StateFlow<WorkerLogUiState> = _uiState

    init {
        viewModelScope.launch {
            workerRunRepository.observeAll().collect { runs ->
                _uiState.value = WorkerLogUiState(runs = runs, isLoading = false)
            }
        }
    }

    private val _detailState = MutableStateFlow(WorkerRunDetailUiState())
    val detailState: StateFlow<WorkerRunDetailUiState> = _detailState

    fun loadDetail(runId: String) {
        viewModelScope.launch {
            _detailState.value = WorkerRunDetailUiState(
                run = workerRunRepository.findById(runId)
            )
        }
    }
}
