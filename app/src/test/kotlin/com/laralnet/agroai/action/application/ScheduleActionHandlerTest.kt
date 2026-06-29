package com.laralnet.agroai.action.application

import com.laralnet.agroai.action.application.command.ScheduleActionCommand
import com.laralnet.agroai.action.application.handler.ScheduleActionHandler
import com.laralnet.agroai.action.domain.event.PlantationActionScheduled
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.calendar.application.handler.CreateCalendarEventHandler
import com.laralnet.agroai.calendar.domain.model.CreatedCalendarEvent
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ScheduleActionHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: PlantationActionRepository = mockk(relaxed = true)
    private val createCalendarEventHandler: CreateCalendarEventHandler = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = ScheduleActionHandler(repository, createCalendarEventHandler, eventBus)

    @Test
    fun `handle() saves action and publishes PlantationActionScheduled`() = runTest {
        val result = handler.handle(command())

        assertTrue(result.isSuccess)
        coVerify { repository.save(any()) }
        coVerify { eventBus.publish(match { it is PlantationActionScheduled }) }
    }

    @Test
    fun `handle() returns action with non-null id`() = runTest {
        val result = handler.handle(command())

        assertNotNull(result.getOrNull()?.id)
    }

    @Test
    fun `handle() does not call createCalendarEventHandler when no account`() = runTest {
        handler.handle(command(calendarEmail = null))

        coVerify(exactly = 0) { createCalendarEventHandler.handle(any()) }
    }

    @Test
    fun `handle() delegates to createCalendarEventHandler when account is provided`() = runTest {
        val created = CreatedCalendarEvent(eventId = 99L, calendarId = 1L, accountEmail = "farmer@gmail.com")
        coEvery { createCalendarEventHandler.handle(any()) } returns Result.success(created)

        val result = handler.handle(command(calendarEmail = "farmer@gmail.com"))

        assertTrue(result.isSuccess)
        coVerify { createCalendarEventHandler.handle(any()) }
        assertEquals(99L, result.getOrNull()?.calendarEventId)
    }

    @Test
    fun `handle() saves action even when calendar event creation fails`() = runTest {
        coEvery { createCalendarEventHandler.handle(any()) } returns Result.failure(Exception("Calendar error"))

        val result = handler.handle(command(calendarEmail = "farmer@gmail.com"))

        assertTrue(result.isSuccess)
        coVerify { repository.save(any()) }
        assertNull(result.getOrNull()?.calendarEventId)
    }

    @Test
    fun `handle() sets source from command`() = runTest {
        val result = handler.handle(command(source = ActionSource.AI))

        assertEquals(ActionSource.AI, result.getOrNull()?.source)
    }

    private fun command(
        calendarEmail: String? = null,
        source: ActionSource = ActionSource.MANUAL
    ) = ScheduleActionCommand(
        plantationId = "plantation-1",
        actionType = ActionType.REGAR,
        title = "Water the tomatoes",
        notes = "",
        scheduledAt = Instant.now(),
        source = source,
        calendarAccountEmail = calendarEmail
    )
}
