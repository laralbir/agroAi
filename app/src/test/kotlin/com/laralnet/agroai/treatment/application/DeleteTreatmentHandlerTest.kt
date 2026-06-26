package com.laralnet.agroai.treatment.application

import com.laralnet.agroai.calendar.application.handler.DeleteCalendarEventHandler
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.treatment.application.command.DeleteTreatmentCommand
import com.laralnet.agroai.treatment.application.handler.DeleteTreatmentHandler
import com.laralnet.agroai.treatment.domain.event.TreatmentDeleted
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DeleteTreatmentHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: TreatmentRepository = mockk(relaxed = true)
    private val deleteCalendarEventHandler: DeleteCalendarEventHandler = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = DeleteTreatmentHandler(repository, deleteCalendarEventHandler, eventBus)

    @Test
    fun `handle() deletes treatment from repository`() = runTest {
        coEvery { repository.findById("t-1") } returns treatment("t-1")

        handler.handle(DeleteTreatmentCommand(treatmentId = "t-1"))

        coVerify { repository.delete("t-1") }
    }

    @Test
    fun `handle() publishes TreatmentDeleted event`() = runTest {
        coEvery { repository.findById("t-1") } returns treatment("t-1")

        handler.handle(DeleteTreatmentCommand(treatmentId = "t-1"))

        coVerify { eventBus.publish(match { it is TreatmentDeleted && (it as TreatmentDeleted).treatmentId == "t-1" }) }
    }

    @Test
    fun `handle() delegates to deleteCalendarEventHandler when calendarEventId is set`() = runTest {
        coEvery { repository.findById("t-1") } returns treatment("t-1", calendarEventId = 99L)

        handler.handle(DeleteTreatmentCommand(treatmentId = "t-1"))

        coVerify { deleteCalendarEventHandler.handle(match { it.eventId == 99L }) }
    }

    @Test
    fun `handle() skips calendar deletion when calendarEventId is null`() = runTest {
        coEvery { repository.findById("t-1") } returns treatment("t-1", calendarEventId = null)

        handler.handle(DeleteTreatmentCommand(treatmentId = "t-1"))

        coVerify(exactly = 0) { deleteCalendarEventHandler.handle(any()) }
    }

    @Test
    fun `handle() returns failure when treatment not found`() = runTest {
        coEvery { repository.findById(any()) } returns null

        val result = handler.handle(DeleteTreatmentCommand(treatmentId = "missing"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    private fun treatment(id: String, calendarEventId: Long? = null) = Treatment(
        id = id,
        plantationId = "plantation-1",
        type = TreatmentType.PODA,
        title = "Pruning",
        scheduledAt = Instant.now(),
        status = TreatmentStatus.PENDING,
        calendarEventId = calendarEventId
    )
}
