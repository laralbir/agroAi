package com.laralnet.agroai.calendar.application.handler

import com.laralnet.agroai.calendar.application.command.CreateCalendarEventCommand
import com.laralnet.agroai.calendar.application.command.DeleteCalendarEventCommand
import com.laralnet.agroai.calendar.application.command.UpdateCalendarEventCommand
import com.laralnet.agroai.calendar.domain.event.CalendarEventCreated
import com.laralnet.agroai.calendar.domain.event.CalendarEventDeleted
import com.laralnet.agroai.calendar.domain.event.CalendarEventUpdated
import com.laralnet.agroai.calendar.domain.model.CalendarEvent
import com.laralnet.agroai.calendar.domain.model.CreatedCalendarEvent
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import javax.inject.Inject

class CreateCalendarEventHandler @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: CreateCalendarEventCommand): Result<CreatedCalendarEvent> = runCatching {
        val calendars = calendarRepository.getCalendars(command.accountEmail)
        val calendar = calendars.firstOrNull { it.isPrimary } ?: calendars.firstOrNull()
            ?: error("No calendar found for ${command.accountEmail}")
        val created = calendarRepository.createEvent(
            CalendarEvent(
                calendarId = calendar.id,
                accountEmail = command.accountEmail,
                title = command.title,
                description = command.description,
                startAt = command.startAt,
                endAt = command.endAt
            )
        ).getOrThrow()
        eventBus.publish(CalendarEventCreated(calendarEventId = created.eventId, treatmentId = command.treatmentId))
        created
    }
}

class UpdateCalendarEventHandler @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: UpdateCalendarEventCommand): Result<Unit> = runCatching {
        calendarRepository.updateEvent(
            eventId = command.eventId,
            event = CalendarEvent(
                calendarId = 0L,
                accountEmail = command.accountEmail,
                title = command.title,
                description = command.description,
                startAt = command.startAt,
                endAt = command.endAt
            )
        ).getOrThrow()
        eventBus.publish(CalendarEventUpdated(calendarEventId = command.eventId))
    }
}

class DeleteCalendarEventHandler @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: DeleteCalendarEventCommand): Result<Unit> = runCatching {
        calendarRepository.deleteEvent(
            calendarId = command.calendarId,
            eventId = command.eventId
        ).getOrThrow()
        eventBus.publish(CalendarEventDeleted(calendarEventId = command.eventId))
    }
}
