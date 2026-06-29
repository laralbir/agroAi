package com.laralnet.agroai.ui.prompteditor

import com.laralnet.agroai.aimodel.application.command.SavePromptTemplateCommand
import com.laralnet.agroai.aimodel.application.handler.SavePromptTemplateHandler
import com.laralnet.agroai.aimodel.application.query.ObservePromptTemplatesQuery
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.model.PromptWarningLevel
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.ui.screens.prompteditor.PromptEditorViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PromptEditorViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: AIModelRepository = mockk(relaxed = true)
    private val handler: SavePromptTemplateHandler = mockk(relaxed = true)

    private fun query(templates: List<PromptTemplate>) = ObservePromptTemplatesQuery(repository).also {
        every { repository.observePromptTemplates() } returns flowOf(templates)
    }

    private fun viewModel(templates: List<PromptTemplate> = listOf(template())): PromptEditorViewModel {
        every { repository.observePromptTemplates() } returns flowOf(templates)
        coEvery { handler.handle(any()) } returns Result.success(Unit)
        return PromptEditorViewModel(
            observePromptTemplatesQuery = ObservePromptTemplatesQuery(repository),
            savePromptTemplateHandler = handler,
            repository = repository
        )
    }

    @Test
    fun `initial load populates templates list`() = runTest {
        val tpl = template("tpl-1", name = "photo_analysis")
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.templates.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `empty DB seeds defaults`() = runTest {
        every { repository.observePromptTemplates() } returns flowOf(emptyList())
        val vm = PromptEditorViewModel(
            observePromptTemplatesQuery = ObservePromptTemplatesQuery(repository),
            savePromptTemplateHandler = handler,
            repository = repository
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        coVerify(atLeast = 1) { repository.savePromptTemplate(any()) }
    }

    @Test
    fun `selectTemplate loads content into editor`() = runTest {
        val tpl = template("tpl-1", content = "original content")
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        advanceUntilIdle()

        assertEquals("tpl-1", vm.uiState.value.selectedId)
        assertEquals("original content", vm.uiState.value.editContent)
        assertFalse(vm.uiState.value.isModified)
    }

    @Test
    fun `onContentChanged marks state as modified`() = runTest {
        val tpl = template("tpl-1", content = "original")
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("modified content")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isModified)
        assertEquals("modified content", vm.uiState.value.editContent)
    }

    @Test
    fun `savePrompt shows warning dialog for MEDIUM level`() = runTest {
        val tpl = template("tpl-1", warningLevel = PromptWarningLevel.MEDIUM)
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("new content")
        vm.savePrompt()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.showWarningDialog)
        coVerify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `savePrompt shows warning dialog for HIGH level`() = runTest {
        val tpl = template("tpl-1", warningLevel = PromptWarningLevel.HIGH)
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("new content")
        vm.savePrompt()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.showWarningDialog)
    }

    @Test
    fun `savePrompt saves immediately for LOW level`() = runTest {
        val tpl = template("tpl-1", warningLevel = PromptWarningLevel.LOW)
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("new content")
        vm.savePrompt()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showWarningDialog)
        coVerify(exactly = 1) { handler.handle(any()) }
    }

    @Test
    fun `confirmSave calls handler and sets savedOk`() = runTest {
        val tpl = template("tpl-1")
        val vm = viewModel(listOf(tpl))
        // Override after viewModel() so this stub wins
        val cmd = slot<SavePromptTemplateCommand>()
        coEvery { handler.handle(capture(cmd)) } returns Result.success(Unit)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("updated")
        vm.confirmSave()
        advanceUntilIdle()

        assertEquals("tpl-1", cmd.captured.templateId)
        assertEquals("updated", cmd.captured.newContent)
        assertTrue(vm.uiState.value.savedOk)
        assertFalse(vm.uiState.value.isModified)
    }

    @Test
    fun `dismissSaveDialog hides dialog`() = runTest {
        val tpl = template("tpl-1", warningLevel = PromptWarningLevel.MEDIUM)
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("new")
        vm.savePrompt()
        vm.dismissSaveDialog()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showWarningDialog)
    }

    @Test
    fun `resetPrompt shows reset dialog`() = runTest {
        val tpl = template("tpl-1")
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.resetPrompt()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.showResetDialog)
    }

    @Test
    fun `confirmReset saves defaultContent and clears isModified`() = runTest {
        val tpl = template("tpl-1", content = "custom content", defaultContent = "factory default")
        val vm = viewModel(listOf(tpl))
        // Override after viewModel() so this stub wins
        val cmd = slot<SavePromptTemplateCommand>()
        coEvery { handler.handle(capture(cmd)) } returns Result.success(Unit)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("custom content")
        vm.confirmReset()
        advanceUntilIdle()

        assertEquals("factory default", cmd.captured.newContent)
        assertEquals("factory default", vm.uiState.value.editContent)
        assertFalse(vm.uiState.value.isModified)
        assertFalse(vm.uiState.value.showResetDialog)
    }

    @Test
    fun `clearSavedOk resets savedOk flag`() = runTest {
        val tpl = template("tpl-1", warningLevel = PromptWarningLevel.LOW)
        val vm = viewModel(listOf(tpl))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.selectTemplate("tpl-1")
        vm.onContentChanged("new")
        vm.confirmSave()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedOk)
        vm.clearSavedOk()
        assertFalse(vm.uiState.value.savedOk)
    }

    private fun template(
        id: String = "tpl-1",
        name: String = "photo_analysis",
        content: String = "original content",
        defaultContent: String = content,
        warningLevel: PromptWarningLevel = PromptWarningLevel.MEDIUM
    ) = PromptTemplate(
        id = id,
        name = name,
        content = content,
        defaultContent = defaultContent,
        warningLevel = warningLevel,
        isEditable = true
    )
}
