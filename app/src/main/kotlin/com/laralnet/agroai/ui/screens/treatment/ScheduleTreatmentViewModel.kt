package com.laralnet.agroai.ui.screens.treatment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.calendar.application.query.GetCalendarsQuery
import com.laralnet.agroai.calendar.domain.model.GoogleCalendar
import com.laralnet.agroai.treatment.application.command.ScheduleTreatmentCommand
import com.laralnet.agroai.treatment.application.handler.ScheduleTreatmentHandler
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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
    private val dataStore: DataStore<Preferences>,
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
        val suggestedDateMillis = extractSuggestedDateMillis(prefillAnalysis ?: prefillDesc)
        ScheduleTreatmentState(
            plantationId = savedStateHandle["plantationId"] ?: "",
            type = prefillType ?: TreatmentType.OTRO,
            title = prefillTitle,
            description = prefillDesc,
            selectedDateMillis = suggestedDateMillis,
            aiAnalysisResult = prefillAnalysis
        )
    })
    val state: StateFlow<ScheduleTreatmentState> = _state.asStateFlow()

    init {
        // Pre-fill the Google Calendar account if the user has configured one and none was passed via nav args
        viewModelScope.launch {
            val savedEmail = dataStore.data
                .map { it[SettingsViewModel.KEY_SELECTED_ACCOUNT]?.ifBlank { null } }
                .first()
            if (savedEmail != null && _state.value.calendarEmail.isBlank()) {
                _state.update { it.copy(calendarEmail = savedEmail, addToCalendar = true) }
            }
        }
    }

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

    companion object {
        /**
         * Extracts a suggested date in epoch millis from AI response text.
         * Priority: ISO date (YYYY-MM-DD) > urgency keywords.
         */
        internal fun extractSuggestedDateMillis(text: String): Long {
            val today = LocalDate.now(ZoneId.systemDefault())

            // 1. Look for ISO date YYYY-MM-DD anywhere in the text
            val isoRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
            isoRegex.findAll(text).forEach { match ->
                runCatching {
                    val date = LocalDate.parse(match.groupValues[1])
                    // Only accept future or today dates
                    if (!date.isBefore(today)) {
                        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                }.onFailure { /* ignore malformed dates */ }
            }

            // 2. Fall back to urgency keywords (EN + ES)
            val lower = text.lowercase()
            return when {
                containsAny(lower, "immediate", "immediately", "urgent", "urgente", "inmediato", "inmediatamente", "hoy", "today") ->
                    today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                containsAny(lower, "this week", "esta semana", "próxima semana", "next week") ->
                    today.plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                containsAny(lower, "this month", "este mes", "próximo mes", "next month") ->
                    today.plusDays(20).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                else -> System.currentTimeMillis()
            }
        }

        private fun containsAny(text: String, vararg keywords: String) =
            keywords.any { text.contains(it) }
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
