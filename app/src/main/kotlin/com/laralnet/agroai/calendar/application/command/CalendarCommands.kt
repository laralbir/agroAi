package com.laralnet.agroai.calendar.application.command

import java.time.Instant

data class CreateCalendarEventCommand(
    val treatmentId: String? = null,
    val accountEmail: String,
    val title: String,
    val description: String = "",
    val startAt: Instant,
    val endAt: Instant
)

data class UpdateCalendarEventCommand(
    val eventId: Long,
    val accountEmail: String,
    val title: String,
    val description: String = "",
    val startAt: Instant,
    val endAt: Instant
)

data class DeleteCalendarEventCommand(
    val eventId: Long,
    val calendarId: Long = 0L
)
