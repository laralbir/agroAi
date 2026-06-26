package com.laralnet.agroai.treatment.application

import com.laralnet.agroai.calendar.application.handler.CreateCalendarEventHandler
import com.laralnet.agroai.calendar.domain.model.CreatedCalendarEvent
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ScheduleTreatmentHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: TreatmentRepository = mockk(relaxed = true)
    private val createCalendarEventHandler: CreateCalendarEventHandler = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = ScheduleTreatmentHandler(repository, createCalendarEventHandler, eventBus)

    @Test
    fun `handle() saves treatment and publishes TreatmentScheduled`() = runTest {
        val result = handler.handle(command())

        assertTrue(result.isSuccess)
        coVerify { repository.save(any()) }
        coVerify { eventBus.publish(match { it is TreatmentScheduled }) }
    }

    @Test
    fun `handle() returns treatment with non-null id`() = runTest {
        val result = handler.handle(command())

        assertNotNull(result.getOrNull()?.id)
    }

    @Test
    fun `handle() does not call createCalendarEventHandler when addToCalendar is false`() = runTest {
        handler.handle(command(addToCalendar = false))

        coVerify(exactly = 0) { createCalendarEventHandler.handle(any()) }
    }

    @Test
    fun `handle() delegates to createCalendarEventHandler when addToCalendar is true`() = runTest {
        val created = CreatedCalendarEvent(eventId = 42L, calendarId = 1L, accountEmail = "test@gmail.com")
        coEvery { createCalendarEventHandler.handle(any()) } returns Result.success(created)

        val result = handler.handle(command(addToCalendar = true, calendarEmail = "test@gmail.com"))

        assertTrue(result.isSuccess)
        coVerify { createCalendarEventHandler.handle(any()) }
        assertTrue(result.getOrNull()?.calendarEventId == 42L)
    }

    @Test
    fun `handle() saves treatment even when calendar event creation fails`() = runTest {
        coEvery { createCalendarEventHandler.handle(any()) } returns Result.failure(Exception("Calendar error"))

        val result = handler.handle(command(addToCalendar = true, calendarEmail = "test@gmail.com"))

        assertTrue(result.isSuccess)
        coVerify { repository.save(any()) }
        assertNull(result.getOrNull()?.calendarEventId)
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
