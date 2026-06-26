package com.laralnet.agroai.calendar.application

import com.laralnet.agroai.calendar.application.command.DeleteCalendarEventCommand
import com.laralnet.agroai.calendar.application.handler.DeleteCalendarEventHandler
import com.laralnet.agroai.calendar.domain.event.CalendarEventDeleted
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeleteCalendarEventHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val calendarRepository: CalendarRepository = mockk()
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = DeleteCalendarEventHandler(calendarRepository, eventBus)

    @Test
    fun `handle() calls deleteEvent on the repository`() = runTest {
        coEvery { calendarRepository.deleteEvent(any(), 99L) } returns Result.success(Unit)

        handler.handle(DeleteCalendarEventCommand(eventId = 99L))

        coVerify { calendarRepository.deleteEvent(any(), 99L) }
    }

    @Test
    fun `handle() publishes CalendarEventDeleted with the correct eventId`() = runTest {
        coEvery { calendarRepository.deleteEvent(any(), 55L) } returns Result.success(Unit)

        handler.handle(DeleteCalendarEventCommand(eventId = 55L))

        coVerify { eventBus.publish(match { it is CalendarEventDeleted && (it as CalendarEventDeleted).calendarEventId == 55L }) }
    }

    @Test
    fun `handle() returns failure when repository throws`() = runTest {
        coEvery { calendarRepository.deleteEvent(any(), any()) } returns Result.failure(RuntimeException("Calendar error"))

        val result = handler.handle(DeleteCalendarEventCommand(eventId = 1L))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { eventBus.publish(any()) }
    }
}
