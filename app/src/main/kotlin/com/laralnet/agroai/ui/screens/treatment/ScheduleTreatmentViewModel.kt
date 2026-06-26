package com.laralnet.agroai.ui.screens.treatment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.calendar.application.query.GetCalendarsQuery
import com.laralnet.agroai.calendar.domain.model.GoogleCalendar
import com.laralnet.agroai.treatment.application.command.ScheduleTreatmentCommand
import com.laralnet.agroai.treatment.application.handler.ScheduleTreatmentHandler
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class ScheduleTreatmentState(
    val plantationId: String = "",
    val type: TreatmentType = TreatmentType.OTRO,
    val title: String = "",
    val description: String = "",
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val hour: Int = 9,
    val minute: Int = 0,
    val addToCalendar: Boolean = false,
    val calendarEmail: String = "",
    val availableCalendars: List<GoogleCalendar> = emptyList(),
    val selectedCalendarId: Long? = null,
    val isLoadingCalendars: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedId: String? = null,
    val aiAnalysisResult: String? = null
)

@HiltViewModel
class ScheduleTreatmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleTreatmentHandler: ScheduleTreatmentHandler,
    private val getCalendarsQuery: GetCalendarsQuery
) : ViewModel() {

    private val _state = MutableStateFlow(run {
        val prefillType = (savedStateHandle.get<String>("prefillType") ?: "").let { raw ->
            TreatmentType.entries.firstOrNull { it.name == raw }
        }
        val prefillTitle = savedStateHandle.get<String>("prefillTitle") ?: ""
        val prefillDesc = savedStateHandle.get<String>("prefillDesc") ?: ""
        val prefillAnalysis = savedStateHandle.get<String>("prefillAnalysis")?.ifBlank { null }
        ScheduleTreatmentState(
            plantationId = savedStateHandle["plantationId"] ?: "",
            type = prefillType ?: TreatmentType.OTRO,
            title = prefillTitle,
            description = prefillDesc,
            aiAnalysisResult = prefillAnalysis
        )
    })
    val state: StateFlow<ScheduleTreatmentState> = _state.asStateFlow()

    fun setType(type: TreatmentType) = _state.update { s ->
        s.copy(type = type, title = if (s.title.isBlank()) type.name else s.title)
    }
    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setDateMillis(millis: Long) = _state.update { it.copy(selectedDateMillis = millis) }
    fun setHour(h: Int) = _state.update { it.copy(hour = h.coerceIn(0, 23)) }
    fun setMinute(m: Int) = _state.update { it.copy(minute = m.coerceIn(0, 59)) }
    fun setAddToCalendar(v: Boolean) = _state.update { it.copy(addToCalendar = v, availableCalendars = emptyList(), selectedCalendarId = null) }
    fun setCalendarEmail(v: String) = _state.update { it.copy(calendarEmail = v, availableCalendars = emptyList(), selectedCalendarId = null) }
    fun selectCalendar(calendarId: Long) = _state.update { it.copy(selectedCalendarId = calendarId) }

    fun loadCalendars() {
        val email = _state.value.calendarEmail.trim()
        if (email.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCalendars = true, error = null) }
            runCatching { getCalendarsQuery(email) }
                .onSuccess { calendars ->
                    val primary = calendars.firstOrNull { it.isPrimary } ?: calendars.firstOrNull()
                    _state.update {
                        it.copy(
                            availableCalendars = calendars,
                            selectedCalendarId = primary?.id,
                            isLoadingCalendars = false,
                            error = if (calendars.isEmpty()) "no_calendars" else null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingCalendars = false, error = e.message) }
                }
        }
    }

    fun schedule() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        val date = Instant.ofEpochMilli(s.selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val scheduledAt = date.atTime(LocalTime.of(s.hour, s.minute))
            .atZone(ZoneId.systemDefault()).toInstant()
        scheduleTreatmentHandler.handle(
            ScheduleTreatmentCommand(
                plantationId = s.plantationId,
                type = s.type,
                title = s.title.ifBlank { s.type.name },
                description = s.description,
                scheduledAt = scheduledAt,
                calendarAccountEmail = s.calendarEmail.ifBlank { null },
                addToCalendar = s.addToCalendar && s.calendarEmail.isNotBlank(),
                aiAnalysisResult = s.aiAnalysisResult
            )
        ).onSuccess { treatment ->
            _state.update { it.copy(isLoading = false, savedId = treatment.id) }
        }.onFailure { e ->
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}
