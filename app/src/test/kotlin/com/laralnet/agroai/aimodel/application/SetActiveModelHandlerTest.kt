package com.laralnet.agroai.aimodel.application

import com.laralnet.agroai.aimodel.application.command.SetActiveModelCommand
import com.laralnet.agroai.aimodel.application.handler.SetActiveModelHandler
import com.laralnet.agroai.aimodel.domain.event.ModelActivated
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetActiveModelHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: AIModelRepository = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = SetActiveModelHandler(repository, eventBus)

    @Test
    fun `handle() calls setActive on repository`() = runTest {
        coEvery { repository.findById("model-1") } returns downloadedModel("model-1")

        handler.handle(SetActiveModelCommand("model-1"))

        coVerify { repository.setActive("model-1") }
    }

    @Test
    fun `handle() publishes ModelActivated event`() = runTest {
        coEvery { repository.findById("model-1") } returns downloadedModel("model-1")

        handler.handle(SetActiveModelCommand("model-1"))

        coVerify { eventBus.publish(match { it is ModelActivated && (it as ModelActivated).modelId == "model-1" }) }
    }

    @Test
    fun `handle() returns failure when model not found`() = runTest {
        coEvery { repository.findById(any()) } returns null

        val result = handler.handle(SetActiveModelCommand("missing"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `handle() returns failure when model is not DOWNLOADED`() = runTest {
        coEvery { repository.findById("model-1") } returns model("model-1", DownloadState.DOWNLOADING)

        val result = handler.handle(SetActiveModelCommand("model-1"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.setActive(any()) }
    }

    private fun downloadedModel(id: String) = model(id, DownloadState.DOWNLOADED)

    private fun model(id: String, state: DownloadState) =
        AIModel(id = id, variant = ModelVariant.GEMMA3N_E2B, version = ModelVariant.GEMMA3N_E2B.gemmaVersion,
            downloadState = state, filePath = "/models/gemma3_1b.task")
}
