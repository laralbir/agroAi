package com.laralnet.agroai.calendar.application

import com.laralnet.agroai.calendar.application.command.UpdateCalendarEventCommand
import com.laralnet.agroai.calendar.application.handler.UpdateCalendarEventHandler
import com.laralnet.agroai.calendar.domain.event.CalendarEventUpdated
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
import java.time.Instant

class UpdateCalendarEventHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val calendarRepository: CalendarRepository = mockk()
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = UpdateCalendarEventHandler(calendarRepository, eventBus)

    private fun command(eventId: Long = 10L) = UpdateCalendarEventCommand(
        eventId = eventId,
        accountEmail = "user@gmail.com",
        title = "Poda actualizada",
        description = "Nueva descripción",
        startAt = Instant.now(),
        endAt = Instant.now().plusSeconds(3600)
    )

    @Test
    fun `handle() calls updateEvent on the repository`() = runTest {
        coEvery { calendarRepository.updateEvent(10L, any()) } returns Result.success(Unit)

        handler.handle(command(eventId = 10L))

        coVerify { calendarRepository.updateEvent(eq(10L), any()) }
    }

    @Test
    fun `handle() publishes CalendarEventUpdated with the correct eventId`() = runTest {
        coEvery { calendarRepository.updateEvent(any(), any()) } returns Result.success(Unit)

        handler.handle(command(eventId = 77L))

        coVerify { eventBus.publish(match { it is CalendarEventUpdated && (it as CalendarEventUpdated).calendarEventId == 77L }) }
    }

    @Test
    fun `handle() returns success when update succeeds`() = runTest {
        coEvery { calendarRepository.updateEvent(any(), any()) } returns Result.success(Unit)

        val result = handler.handle(command())

        assertTrue(result.isSuccess)
    }

    @Test
    fun `handle() returns failure and does not publish event when update fails`() = runTest {
        coEvery { calendarRepository.updateEvent(any(), any()) } returns Result.failure(RuntimeException("error"))

        val result = handler.handle(command())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { eventBus.publish(any()) }
    }
}
