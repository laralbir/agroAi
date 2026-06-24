package com.laralnet.agroai.calendar.domain.repository

import com.laralnet.agroai.calendar.domain.model.CalendarEvent
import com.laralnet.agroai.calendar.domain.model.CreatedCalendarEvent
import com.laralnet.agroai.calendar.domain.model.GoogleCalendar

interface CalendarRepository {
    suspend fun getCalendars(accountEmail: String): List<GoogleCalendar>
    suspend fun createEvent(event: CalendarEvent): Result<CreatedCalendarEvent>
    suspend fun deleteEvent(calendarId: Long, eventId: Long): Result<Unit>
    suspend fun updateEvent(eventId: Long, event: CalendarEvent): Result<Unit>
}
