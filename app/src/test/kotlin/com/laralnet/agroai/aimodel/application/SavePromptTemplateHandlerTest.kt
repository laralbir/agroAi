package com.laralnet.agroai.aimodel.application

import com.laralnet.agroai.aimodel.application.command.SavePromptTemplateCommand
import com.laralnet.agroai.aimodel.application.handler.SavePromptTemplateHandler
import com.laralnet.agroai.aimodel.domain.event.PromptTemplateUpdated
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.model.PromptWarningLevel
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
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

class SavePromptTemplateHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: AIModelRepository = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = SavePromptTemplateHandler(repository, eventBus)

    @Test
    fun `handle() saves updated content`() = runTest {
        coEvery { repository.findPromptTemplateById("tpl-1") } returns template("tpl-1")
        val saved = slot<PromptTemplate>()
        coEvery { repository.savePromptTemplate(capture(saved)) } returns Unit

        handler.handle(SavePromptTemplateCommand("tpl-1", "new content"))

        assertEquals("new content", saved.captured.content)
    }

    @Test
    fun `handle() marks template as customized`() = runTest {
        coEvery { repository.findPromptTemplateById("tpl-1") } returns template("tpl-1")
        val saved = slot<PromptTemplate>()
        coEvery { repository.savePromptTemplate(capture(saved)) } returns Unit

        handler.handle(SavePromptTemplateCommand("tpl-1", "new content"))

        assertTrue(saved.captured.isCustomized)
    }

    @Test
    fun `handle() publishes PromptTemplateUpdated event`() = runTest {
        coEvery { repository.findPromptTemplateById("tpl-1") } returns template("tpl-1")

        handler.handle(SavePromptTemplateCommand("tpl-1", "new content"))

        coVerify { eventBus.publish(match { it is PromptTemplateUpdated }) }
    }

    @Test
    fun `handle() returns failure when template not found`() = runTest {
        coEvery { repository.findPromptTemplateById(any()) } returns null

        val result = handler.handle(SavePromptTemplateCommand("missing", "content"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.savePromptTemplate(any()) }
    }

    @Test
    fun `handle() returns failure when template is not editable`() = runTest {
        coEvery { repository.findPromptTemplateById("tpl-1") } returns template("tpl-1", editable = false)

        val result = handler.handle(SavePromptTemplateCommand("tpl-1", "content"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.savePromptTemplate(any()) }
    }

    private fun template(
        id: String,
        editable: Boolean = true,
        warningLevel: PromptWarningLevel = PromptWarningLevel.MEDIUM
    ) = PromptTemplate(
        id = id,
        name = "photo_analysis",
        content = "original content",
        isEditable = editable,
        warningLevel = warningLevel
    )
}
