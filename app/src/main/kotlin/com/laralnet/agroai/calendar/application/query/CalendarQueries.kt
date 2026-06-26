package com.laralnet.agroai.calendar.application.query

import com.laralnet.agroai.calendar.domain.model.GoogleCalendar
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import javax.inject.Inject

class GetCalendarsQuery @Inject constructor(
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(accountEmail: String): List<GoogleCalendar> =
        calendarRepository.getCalendars(accountEmail)
}
