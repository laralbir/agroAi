package com.laralnet.agroai.treatment.application

import com.laralnet.agroai.calendar.domain.model.CreatedCalendarEvent
import com.laralnet.agroai.calendar.domain.model.GoogleCalendar
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.treatment.application.command.ScheduleTreatmentCommand
import com.laralnet.agroai.treatment.application.handler.ScheduleTreatmentHandler
import com.laralnet.agroai.treatment.domain.event.TreatmentScheduled
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ScheduleTreatmentHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: TreatmentRepository = mockk(relaxed = true)
    private val calendarRepository: CalendarRepository = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = ScheduleTreatmentHandler(repository, calendarRepository, eventBus)

    @Test
    fun `handle() saves treatment and publishes event`() = runTest {
        val result = handler.handle(command())

        assertTrue(result.isSuccess)
        coVerify { repository.save(any()) }
        coVerify { eventBus.publish(match { it is TreatmentScheduled }) }
    }

    @Test
    fun `handle() returns treatment with id`() = runTest {
        val result = handler.handle(command())

        assertNotNull(result.getOrNull()?.id)
    }

    @Test
    fun `handle() does not call calendarRepository when addToCalendar is false`() = runTest {
        handler.handle(command(addToCalendar = false))

        coVerify(exactly = 0) { calendarRepository.getCalendars(any()) }
        coVerify(exactly = 0) { calendarRepository.createEvent(any()) }
    }

    @Test
    fun `handle() creates calendar event when addToCalendar is true`() = runTest {
        val calendar = GoogleCalendar(id = 1L, accountEmail = "test@gmail.com", displayName = "Test", isPrimary = true)
        val createdEvent = CreatedCalendarEvent(eventId = 42L, calendarId = 1L, accountEmail = "test@gmail.com")
        coEvery { calendarRepository.getCalendars("test@gmail.com") } returns listOf(calendar)
        coEvery { calendarRepository.createEvent(any()) } returns Result.success(createdEvent)

        val result = handler.handle(command(addToCalendar = true, calendarEmail = "test@gmail.com"))

        assertTrue(result.isSuccess)
        coVerify { calendarRepository.createEvent(any()) }
        assertTrue(result.getOrNull()?.calendarEventId == 42L)
    }

    @Test
    fun `handle() saves treatment even when calendar event creation fails`() = runTest {
        val calendar = GoogleCalendar(id = 1L, accountEmail = "test@gmail.com", displayName = "Test", isPrimary = true)
        coEvery { calendarRepository.getCalendars("test@gmail.com") } returns listOf(calendar)
        coEvery { calendarRepository.createEvent(any()) } returns Result.failure(Exception("Calendar error"))

        val result = handler.handle(command(addToCalendar = true, calendarEmail = "test@gmail.com"))

        assertTrue(result.isSuccess)
        coVerify { repository.save(any()) }
    }

    private fun command(
        addToCalendar: Boolean = false,
        calendarEmail: String? = null
    ) = ScheduleTreatmentCommand(
        plantationId = "plantation-1",
        type = TreatmentType.RIEGO,
        title = "Weekly irrigation",
        description = "",
        scheduledAt = Instant.now(),
        calendarAccountEmail = calendarEmail,
        addToCalendar = addToCalendar
    )
}
