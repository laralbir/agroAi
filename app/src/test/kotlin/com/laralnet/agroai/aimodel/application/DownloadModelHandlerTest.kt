package com.laralnet.agroai.aimodel.application

import com.laralnet.agroai.aimodel.application.command.DownloadModelCommand
import com.laralnet.agroai.aimodel.application.handler.DownloadModelHandler
import com.laralnet.agroai.aimodel.application.port.ModelDownloader
import com.laralnet.agroai.aimodel.domain.event.ModelDownloadStarted
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DownloadModelHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: AIModelRepository = mockk(relaxed = true)
    private val downloader: ModelDownloader = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = DownloadModelHandler(repository, downloader, eventBus)

    private val command = DownloadModelCommand(variant = ModelVariant.GEMMA3N_E2B)

    @Test
    fun `handle() saves model with DOWNLOADING state`() = runTest {
        coEvery { repository.findByVariant(any()) } returns null

        handler.handle(command)

        coVerify { repository.save(match { it.downloadState == DownloadState.DOWNLOADING }) }
    }

    @Test
    fun `handle() enqueues download job`() = runTest {
        coEvery { repository.findByVariant(any()) } returns null

        handler.handle(command)

        coVerify { downloader.enqueue(any(), ModelVariant.GEMMA3N_E2B) }
    }

    @Test
    fun `handle() publishes ModelDownloadStarted event`() = runTest {
        coEvery { repository.findByVariant(any()) } returns null

        handler.handle(command)

        coVerify { eventBus.publish(match { it is ModelDownloadStarted }) }
    }

    @Test
    fun `handle() returns same id if model already DOWNLOADED`() = runTest {
        val existing = model(id = "existing-id", state = DownloadState.DOWNLOADED)
        coEvery { repository.findByVariant(ModelVariant.GEMMA3N_E2B) } returns existing

        val result = handler.handle(command)

        assertEquals("existing-id", result.getOrThrow())
        coVerify(exactly = 0) { repository.save(any()) }
        coVerify(exactly = 0) { downloader.enqueue(any(), any()) }
    }

    @Test
    fun `handle() reuses existing id if model is FAILED`() = runTest {
        val existing = model(id = "failed-id", state = DownloadState.FAILED)
        coEvery { repository.findByVariant(ModelVariant.GEMMA3N_E2B) } returns existing

        val result = handler.handle(command)

        assertTrue(result.isSuccess)
        coVerify { repository.save(match { it.id == "failed-id" }) }
    }

    @Test
    fun `handle() returns failure when repository throws`() = runTest {
        coEvery { repository.findByVariant(any()) } throws RuntimeException("DB error")

        val result = handler.handle(command)

        assertTrue(result.isFailure)
    }

    private fun model(id: String = "id", state: DownloadState = DownloadState.NOT_DOWNLOADED) =
        AIModel(id = id, variant = ModelVariant.GEMMA3N_E2B, version = ModelVariant.GEMMA3N_E2B.gemmaVersion, downloadState = state)
}
