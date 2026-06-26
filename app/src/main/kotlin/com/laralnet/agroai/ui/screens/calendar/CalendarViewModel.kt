package com.laralnet.agroai.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.treatment.application.query.ObserveTreatmentsByMonthQuery
import com.laralnet.agroai.treatment.domain.model.Treatment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate = LocalDate.now(),
    val treatmentsByDay: Map<LocalDate, List<Treatment>> = emptyMap(),
    val selectedDayTreatments: List<Treatment> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeTreatmentsByMonth: ObserveTreatmentsByMonthQuery
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDay = MutableStateFlow(LocalDate.now())

    private val monthTreatments = _currentMonth
        .flatMapLatest { month -> observeTreatmentsByMonth(month) }

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth, _selectedDay, monthTreatments
    ) { month, selected, treatments ->
        val zone = ZoneId.systemDefault()
        val byDay = treatments.groupBy { t -> t.scheduledAt.atZone(zone).toLocalDate() }
        CalendarUiState(
            currentMonth = month,
            selectedDay = selected,
            treatmentsByDay = byDay,
            selectedDayTreatments = byDay[selected] ?: emptyList()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarUiState(currentMonth = _currentMonth.value, selectedDay = _selectedDay.value)
    )

    fun previousMonth() {
        _currentMonth.update { it.minusMonths(1) }
        _selectedDay.update { it.minusMonths(1).withDayOfMonth(1) }
    }

    fun nextMonth() {
        _currentMonth.update { it.plusMonths(1) }
        _selectedDay.update { it.plusMonths(1).withDayOfMonth(1) }
    }

    fun selectDay(day: LocalDate) = _selectedDay.update { day }
}
