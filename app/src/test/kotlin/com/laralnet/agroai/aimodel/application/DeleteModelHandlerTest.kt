package com.laralnet.agroai.aimodel.application

import com.laralnet.agroai.aimodel.application.command.DeleteModelCommand
import com.laralnet.agroai.aimodel.application.handler.DeleteModelHandler
import com.laralnet.agroai.aimodel.application.port.ModelDownloader
import com.laralnet.agroai.aimodel.domain.event.ModelDeleted
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeleteModelHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: AIModelRepository = mockk(relaxed = true)
    private val downloader: ModelDownloader = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = DeleteModelHandler(repository, downloader, eventBus)

    @Test
    fun `handle() deletes model from repository`() = runTest {
        coEvery { repository.findById("model-1") } returns model("model-1", DownloadState.DOWNLOADED)

        handler.handle(DeleteModelCommand("model-1"))

        coVerify { repository.delete("model-1") }
    }

    @Test
    fun `handle() publishes ModelDeleted event`() = runTest {
        coEvery { repository.findById("model-1") } returns model("model-1", DownloadState.DOWNLOADED)

        handler.handle(DeleteModelCommand("model-1"))

        coVerify { eventBus.publish(match { it is ModelDeleted && (it as ModelDeleted).modelId == "model-1" }) }
    }

    @Test
    fun `handle() cancels download when model is DOWNLOADING`() = runTest {
        coEvery { repository.findById("model-1") } returns model("model-1", DownloadState.DOWNLOADING)

        handler.handle(DeleteModelCommand("model-1"))

        verify { downloader.cancel("model-1") }
    }

    @Test
    fun `handle() does not cancel download when model is DOWNLOADED`() = runTest {
        coEvery { repository.findById("model-1") } returns model("model-1", DownloadState.DOWNLOADED)

        handler.handle(DeleteModelCommand("model-1"))

        verify(exactly = 0) { downloader.cancel(any()) }
    }

    @Test
    fun `handle() returns failure when model not found`() = runTest {
        coEvery { repository.findById(any()) } returns null

        val result = handler.handle(DeleteModelCommand("missing"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    private fun model(id: String, state: DownloadState) =
        AIModel(id = id, variant = ModelVariant.GEMMA3_1B, version = ModelVariant.GEMMA3_1B.gemmaVersion, downloadState = state)
}
