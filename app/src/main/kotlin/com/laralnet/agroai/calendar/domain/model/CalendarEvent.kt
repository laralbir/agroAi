package com.laralnet.agroai.calendar.domain.model

import java.time.Instant

data class GoogleCalendar(
    val id: Long,
    val accountEmail: String,
    val displayName: String,
    val isPrimary: Boolean
)

data class CalendarEvent(
    val calendarId: Long,
    val accountEmail: String,
    val title: String,
    val description: String = "",
    val startAt: Instant,
    val endAt: Instant,
    val allDay: Boolean = false,
    val remindersMinutes: List<Int> = listOf(60, 1440)
)

data class CreatedCalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val accountEmail: String
)
