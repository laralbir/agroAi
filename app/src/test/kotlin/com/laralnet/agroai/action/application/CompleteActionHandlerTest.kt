package com.laralnet.agroai.action.application

import com.laralnet.agroai.action.application.command.CompleteActionCommand
import com.laralnet.agroai.action.application.handler.CompleteActionHandler
import com.laralnet.agroai.action.domain.event.PlantationActionCompleted
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CompleteActionHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: PlantationActionRepository = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = CompleteActionHandler(repository, eventBus)

    @Test
    fun `handle() marks action as DONE`() = runTest {
        val action = testAction()
        coEvery { repository.findById(action.id) } returns action
        val saved = slot<PlantationAction>()
        coEvery { repository.save(capture(saved)) } returns Unit

        val result = handler.handle(CompleteActionCommand(id = action.id))

        assertTrue(result.isSuccess)
        assertEquals(ActionStatus.DONE, saved.captured.status)
    }

    @Test
    fun `handle() publishes PlantationActionCompleted`() = runTest {
        val action = testAction()
        coEvery { repository.findById(action.id) } returns action

        handler.handle(CompleteActionCommand(id = action.id))

        coVerify { eventBus.publish(match { it is PlantationActionCompleted }) }
    }

    @Test
    fun `handle() returns failure when action not found`() = runTest {
        coEvery { repository.findById(any()) } returns null

        val result = handler.handle(CompleteActionCommand(id = "unknown"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `handle() saves with correct plantation id`() = runTest {
        val action = testAction(plantationId = "my-plantation")
        coEvery { repository.findById(action.id) } returns action
        val saved = slot<PlantationAction>()
        coEvery { repository.save(capture(saved)) } returns Unit

        handler.handle(CompleteActionCommand(id = action.id))

        assertEquals("my-plantation", saved.captured.plantationId)
    }

    private fun testAction(plantationId: String = "plantation-1") = PlantationAction(
        id = "action-1",
        plantationId = plantationId,
        actionType = ActionType.REGAR,
        title = "Water the tomatoes",
        scheduledAt = Instant.now(),
        status = ActionStatus.PENDING,
        source = ActionSource.MANUAL
    )
}
