package com.laralnet.agroai.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.action.application.query.ObserveActionsByPlantationQuery
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReportDateRange(val from: LocalDate? = null, val to: LocalDate? = null)

data class PlantationReportUiState(
    val plantationId: String = "",
    val plantationName: String = "",
    val completedActions: List<PlantationAction> = emptyList(),
    val pendingActions: List<PlantationAction> = emptyList(),
    val filterActionType: ActionType? = null,
    val filterDateRange: ReportDateRange = ReportDateRange(),
    val filterPlantTypeId: String? = null,
    val exportContent: String = ""
)

@HiltViewModel
class PlantationReportViewModel @Inject constructor(
    private val observeActionsQuery: ObserveActionsByPlantationQuery,
    private val plantationRepository: PlantationRepository
) : ViewModel() {

    private val plantationId = MutableStateFlow("")
    private val filterActionType = MutableStateFlow<ActionType?>(null)
    private val filterDateRange = MutableStateFlow(ReportDateRange())
    private val filterPlantTypeId = MutableStateFlow<String?>(null)

    private val allActions = plantationId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList()) else observeActionsQuery(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<PlantationReportUiState> = combine(
        allActions,
        filterActionType,
        filterDateRange,
        filterPlantTypeId
    ) { actions, actionType, dateRange, plantTypeId ->
        val filtered = actions
            .filter { actionType == null || it.actionType == actionType }
            .filter { plantTypeId == null || it.plantTypeId == plantTypeId }
            .filter { action ->
                val actionDate = action.scheduledAt.atZone(ZoneId.systemDefault()).toLocalDate()
                (dateRange.from == null || !actionDate.isBefore(dateRange.from)) &&
                    (dateRange.to == null || !actionDate.isAfter(dateRange.to))
            }

        val completed = filtered.filter { it.status == ActionStatus.DONE }
            .sortedByDescending { it.scheduledAt }
        val pending = filtered.filter { it.status == ActionStatus.PENDING }
            .sortedBy { it.scheduledAt }

        PlantationReportUiState(
            plantationId = plantationId.value,
            completedActions = completed,
            pendingActions = pending,
            filterActionType = actionType,
            filterDateRange = dateRange,
            filterPlantTypeId = plantTypeId,
            exportContent = buildExportMarkdown(plantationId.value, completed, pending)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlantationReportUiState())

    fun load(id: String) {
        plantationId.value = id
        viewModelScope.launch {
            val plantation = plantationRepository.findById(id) ?: return@launch
            // Update state with name — done via separate StateFlow merge if needed,
            // but for simplicity we update the flow source and let uiState recompute.
            // We expose plantationName separately via a dedicated state.
        }
    }

    val plantationName: StateFlow<String> = plantationId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf("") else
                kotlinx.coroutines.flow.flow {
                    emit(plantationRepository.findById(id)?.name ?: "")
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setActionTypeFilter(type: ActionType?) {
        filterActionType.value = type
    }

    fun setDateRangeFilter(range: ReportDateRange) {
        filterDateRange.value = range
    }

    fun setPlantTypeFilter(plantTypeId: String?) {
        filterPlantTypeId.value = plantTypeId
    }

    fun clearFilters() {
        filterActionType.value = null
        filterDateRange.value = ReportDateRange()
        filterPlantTypeId.value = null
    }
}

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

private fun buildExportMarkdown(
    plantationId: String,
    completed: List<PlantationAction>,
    pending: List<PlantationAction>
): String = buildString {
    append("# Plantation Report\n\n")
    append("**Generated:** ${LocalDate.now(ZoneId.systemDefault()).format(dateFmt)}\n\n")

    append("## Completed Actions (${completed.size})\n\n")
    if (completed.isEmpty()) {
        append("_No completed actions._\n\n")
    } else {
        completed.forEach { action ->
            val date = action.scheduledAt.atZone(ZoneId.systemDefault())
                .toLocalDate().format(dateFmt)
            val sourceIcon = when (action.source) {
                ActionSource.MANUAL -> "✋"
                ActionSource.AI -> "🤖"
                ActionSource.PHOTO_AI -> "📷"
            }
            append("- **${action.title}** — $date $sourceIcon")
            if (action.notes.isNotBlank()) append("\n  _${action.notes}_")
            append("\n")
        }
        append("\n")
    }

    append("## Pending Actions (${pending.size})\n\n")
    if (pending.isEmpty()) {
        append("_No pending actions._\n\n")
    } else {
        pending.forEach { action ->
            val date = action.scheduledAt.atZone(ZoneId.systemDefault())
                .toLocalDate().format(dateFmt)
            val sourceIcon = when (action.source) {
                ActionSource.MANUAL -> "✋"
                ActionSource.AI -> "🤖"
                ActionSource.PHOTO_AI -> "📷"
            }
            append("- **${action.title}** — $date $sourceIcon")
            if (action.notes.isNotBlank()) append("\n  _${action.notes}_")
            append("\n")
        }
    }
}
