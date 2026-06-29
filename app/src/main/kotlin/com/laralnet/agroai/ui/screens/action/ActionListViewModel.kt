package com.laralnet.agroai.ui.screens.action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.action.application.command.ScheduleActionCommand
import com.laralnet.agroai.action.application.handler.DeleteActionHandler
import com.laralnet.agroai.action.application.handler.ScheduleActionHandler
import com.laralnet.agroai.action.application.query.ObserveActionsByPlantationQuery
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class ActionFilter { ALL, PENDING, DONE, SKIPPED }

data class ActionListUiState(
    val actions: List<PlantationAction> = emptyList(),
    val filter: ActionFilter = ActionFilter.ALL,
    val plantationId: String = ""
)

@HiltViewModel
class ActionListViewModel @Inject constructor(
    private val observeActionsQuery: ObserveActionsByPlantationQuery,
    private val scheduleActionHandler: ScheduleActionHandler,
    private val deleteActionHandler: DeleteActionHandler,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val plantationId = MutableStateFlow("")
    private val filter = MutableStateFlow(ActionFilter.ALL)

    private val calendarAccount = dataStore.data
        .map { it[KEY_CALENDAR_ACCOUNT]?.ifBlank { null } }

    private val allActions: StateFlow<List<PlantationAction>> = plantationId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList()) else observeActionsQuery(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ActionListUiState> = combine(allActions, filter, plantationId) { actions, f, id ->
        val filtered = when (f) {
            ActionFilter.ALL -> actions
            ActionFilter.PENDING -> actions.filter { it.status == ActionStatus.PENDING }
            ActionFilter.DONE -> actions.filter { it.status == ActionStatus.DONE }
            ActionFilter.SKIPPED -> actions.filter { it.status == ActionStatus.SKIPPED }
        }
        ActionListUiState(actions = filtered, filter = f, plantationId = id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActionListUiState())

    fun load(id: String) {
        plantationId.value = id
    }

    fun setFilter(f: ActionFilter) {
        filter.value = f
    }

    fun scheduleAction(
        actionType: ActionType,
        title: String,
        notes: String,
        scheduledAt: Instant
    ) = viewModelScope.launch {
        val account = calendarAccount.map { it }.stateIn(viewModelScope, SharingStarted.Eagerly, null).value
        scheduleActionHandler.handle(
            ScheduleActionCommand(
                plantationId = plantationId.value,
                actionType = actionType,
                title = title,
                notes = notes,
                scheduledAt = scheduledAt,
                source = ActionSource.MANUAL,
                calendarAccountEmail = account
            )
        )
    }

    fun deleteAction(id: String) = viewModelScope.launch {
        deleteActionHandler.handle(com.laralnet.agroai.action.application.command.DeleteActionCommand(id))
    }

    companion object {
        private val KEY_CALENDAR_ACCOUNT = stringPreferencesKey("selected_google_account")
    }
}
