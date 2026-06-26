package com.laralnet.agroai.treatment.application

import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.treatment.application.command.CompleteTreatmentCommand
import com.laralnet.agroai.treatment.application.handler.CompleteTreatmentHandler
import com.laralnet.agroai.treatment.domain.event.TreatmentCompleted
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

class CompleteTreatmentHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: TreatmentRepository = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = CompleteTreatmentHandler(repository, eventBus)

    @Test
    fun `handle() marks treatment as DONE`() = runTest {
        coEvery { repository.findById("t-1") } returns treatment("t-1")

        handler.handle(CompleteTreatmentCommand(treatmentId = "t-1"))

        coVerify { repository.save(match { it.id == "t-1" && it.status == TreatmentStatus.DONE }) }
    }

    @Test
    fun `handle() saves a completion record`() = runTest {
        coEvery { repository.findById("t-1") } returns treatment("t-1")

        handler.handle(CompleteTreatmentCommand(treatmentId = "t-1", notes = "All done"))

        coVerify { repository.saveRecord(match { it.treatmentId == "t-1" && it.notes == "All done" }) }
    }

    @Test
    fun `handle() publishes TreatmentCompleted event`() = runTest {
        coEvery { repository.findById("t-1") } returns treatment("t-1")

        handler.handle(CompleteTreatmentCommand(treatmentId = "t-1"))

        coVerify { eventBus.publish(match { it is TreatmentCompleted && (it as TreatmentCompleted).treatmentId == "t-1" }) }
    }

    @Test
    fun `handle() returns failure when treatment not found`() = runTest {
        coEvery { repository.findById(any()) } returns null

        val result = handler.handle(CompleteTreatmentCommand(treatmentId = "missing"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    private fun treatment(id: String) = Treatment(
        id = id,
        plantationId = "plantation-1",
        type = TreatmentType.RIEGO,
        title = "Irrigation",
        scheduledAt = Instant.now(),
        status = TreatmentStatus.PENDING
    )
}
