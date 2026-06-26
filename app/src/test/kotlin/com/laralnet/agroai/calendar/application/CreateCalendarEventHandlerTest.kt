package com.laralnet.agroai.calendar.application

import com.laralnet.agroai.calendar.application.command.CreateCalendarEventCommand
import com.laralnet.agroai.calendar.application.handler.CreateCalendarEventHandler
import com.laralnet.agroai.calendar.domain.event.CalendarEventCreated
import com.laralnet.agroai.calendar.domain.model.CreatedCalendarEvent
import com.laralnet.agroai.calendar.domain.model.GoogleCalendar
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CreateCalendarEventHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val calendarRepository: CalendarRepository = mockk()
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = CreateCalendarEventHandler(calendarRepository, eventBus)

    private val primaryCalendar = GoogleCalendar(id = 1L, accountEmail = "user@gmail.com", displayName = "Personal", isPrimary = true)
    private val secondaryCalendar = GoogleCalendar(id = 2L, accountEmail = "user@gmail.com", displayName = "Work", isPrimary = false)
    private val createdEvent = CreatedCalendarEvent(eventId = 42L, calendarId = 1L, accountEmail = "user@gmail.com")

    private fun command(treatmentId: String? = null) = CreateCalendarEventCommand(
        treatmentId = treatmentId,
        accountEmail = "user@gmail.com",
        title = "Riego",
        description = "Riego semanal",
        startAt = Instant.now(),
        endAt = Instant.now().plusSeconds(3600)
    )

    @Test
    fun `handle() creates event in the primary calendar`() = runTest {
        coEvery { calendarRepository.getCalendars("user@gmail.com") } returns listOf(primaryCalendar, secondaryCalendar)
        coEvery { calendarRepository.createEvent(any()) } returns Result.success(createdEvent)

        val result = handler.handle(command())

        assertTrue(result.isSuccess)
        coVerify { calendarRepository.createEvent(match { it.calendarId == 1L }) }
    }

    @Test
    fun `handle() falls back to first calendar when no primary`() = runTest {
        val nonPrimary = secondaryCalendar.copy(isPrimary = false)
        coEvery { calendarRepository.getCalendars("user@gmail.com") } returns listOf(nonPrimary)
        coEvery { calendarRepository.createEvent(any()) } returns Result.success(createdEvent)

        val result = handler.handle(command())

        assertTrue(result.isSuccess)
        coVerify { calendarRepository.createEvent(match { it.calendarId == 2L }) }
    }

    @Test
    fun `handle() publishes CalendarEventCreated with correct eventId and treatmentId`() = runTest {
        coEvery { calendarRepository.getCalendars(any()) } returns listOf(primaryCalendar)
        coEvery { calendarRepository.createEvent(any()) } returns Result.success(createdEvent)

        handler.handle(command(treatmentId = "t-123"))

        coVerify {
            eventBus.publish(match {
                it is CalendarEventCreated && it.calendarEventId == 42L && it.treatmentId == "t-123"
            })
        }
    }

    @Test
    fun `handle() returns failure when no calendars found`() = runTest {
        coEvery { calendarRepository.getCalendars(any()) } returns emptyList()

        val result = handler.handle(command())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { calendarRepository.createEvent(any()) }
    }

    @Test
    fun `handle() returns failure when createEvent fails`() = runTest {
        coEvery { calendarRepository.getCalendars(any()) } returns listOf(primaryCalendar)
        coEvery { calendarRepository.createEvent(any()) } returns Result.failure(RuntimeException("Calendar error"))

        val result = handler.handle(command())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { eventBus.publish(any()) }
    }

    @Test
    fun `handle() returns the created event on success`() = runTest {
        coEvery { calendarRepository.getCalendars(any()) } returns listOf(primaryCalendar)
        coEvery { calendarRepository.createEvent(any()) } returns Result.success(createdEvent)

        val result = handler.handle(command())

        assertEquals(42L, result.getOrNull()?.eventId)
    }
}
