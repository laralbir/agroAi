package com.laralnet.agroai.ui.aimodel

import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import com.laralnet.agroai.aimodel.application.handler.DeleteModelHandler
import com.laralnet.agroai.aimodel.application.handler.DownloadModelHandler
import com.laralnet.agroai.aimodel.application.handler.SetActiveModelHandler
import com.laralnet.agroai.aimodel.application.query.ObserveModelsQuery
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.ui.screens.aimodel.ModelManagementViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ModelManagementViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val observeModels: ObserveModelsQuery = mockk()
    private val downloadHandler: DownloadModelHandler = mockk(relaxed = true)
    private val setActiveHandler: SetActiveModelHandler = mockk(relaxed = true)
    private val deleteHandler: DeleteModelHandler = mockk(relaxed = true)
    private val workManager: WorkManager = mockk {
        every { getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())
    }

    private fun viewModel(): ModelManagementViewModel = ModelManagementViewModel(
        observeModels, downloadHandler, setActiveHandler, deleteHandler, workManager
    )

    private fun model(
        variant: ModelVariant = ModelVariant.GEMMA3_1B,
        state: DownloadState = DownloadState.NOT_DOWNLOADED,
        isActive: Boolean = false,
        progress: Int = 0
    ) = AIModel(
        id = "id-${variant.name}",
        variant = variant,
        version = variant.gemmaVersion,
        downloadState = state,
        downloadProgressPercent = progress,
        isActive = isActive
    )

    @Test
    fun `initial state shows all ModelVariant entries`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        val vm = viewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(ModelVariant.entries.size, state.rows.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `NOT_DOWNLOADED variant has correct downloadState`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        val vm = viewModel()

        vm.uiState.test {
            val row = awaitItem().rows.first { it.variant == ModelVariant.GEMMA3_1B }
            assertEquals(DownloadState.NOT_DOWNLOADED, row.downloadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DOWNLOADED model shows correct state`() = runTest {
        every { observeModels() } returns flowOf(
            listOf(model(variant = ModelVariant.GEMMA3_1B, state = DownloadState.DOWNLOADED))
        )
        val vm = viewModel()

        vm.uiState.test {
            val row = awaitItem().rows.first { it.variant == ModelVariant.GEMMA3_1B }
            assertEquals(DownloadState.DOWNLOADED, row.downloadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active model shows isActive=true`() = runTest {
        every { observeModels() } returns flowOf(
            listOf(model(variant = ModelVariant.GEMMA3_1B, state = DownloadState.DOWNLOADED, isActive = true))
        )
        val vm = viewModel()

        vm.uiState.test {
            val row = awaitItem().rows.first { it.variant == ModelVariant.GEMMA3_1B }
            assertTrue(row.isActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download click for small model (less than 12GB) calls handler immediately`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        coEvery { downloadHandler.handle(any()) } returns Result.success("id-1")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadClick(ModelVariant.GEMMA3_1B)
        advanceUntilIdle()

        coVerify { downloadHandler.handle(any()) }
    }

    @Test
    fun `download click for large model (12GB+) shows warning dialog`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadClick(ModelVariant.GEMMA3_12B)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(ModelVariant.GEMMA3_12B, state.pendingWarningVariant)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `warning confirmed clears dialog and calls download handler`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        coEvery { downloadHandler.handle(any()) } returns Result.success("id-1")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadClick(ModelVariant.GEMMA3_12B)
        vm.onWarningConfirmed()
        advanceUntilIdle()

        assertNull(vm.uiState.value.pendingWarningVariant)
        coVerify { downloadHandler.handle(any()) }
    }

    @Test
    fun `warning dismissed clears dialog without downloading`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadClick(ModelVariant.GEMMA3_12B)
        vm.onWarningDismissed()

        assertNull(vm.uiState.value.pendingWarningVariant)
        coVerify(exactly = 0) { downloadHandler.handle(any()) }
    }

    @Test
    fun `onActivate calls SetActiveModelHandler`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onActivate("model-id")
        advanceUntilIdle()

        coVerify { setActiveHandler.handle(any()) }
    }

    @Test
    fun `onDelete calls DeleteModelHandler`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDelete("model-id")
        advanceUntilIdle()

        coVerify { deleteHandler.handle(any()) }
    }

    @Test
    fun `handler failure sets error in state`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        coEvery { downloadHandler.handle(any()) } returns Result.failure(RuntimeException("Network error"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadClick(ModelVariant.GEMMA3_1B)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error!!.contains("Network error"))
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        coEvery { downloadHandler.handle(any()) } returns Result.failure(RuntimeException("err"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDownloadClick(ModelVariant.GEMMA3_1B)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        vm.clearError()

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `rows update reactively when model list changes`() = runTest {
        val flow = MutableStateFlow<List<AIModel>>(emptyList())
        every { observeModels() } returns flow
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(
            DownloadState.NOT_DOWNLOADED,
            vm.uiState.value.rows.first { it.variant == ModelVariant.GEMMA3_1B }.downloadState
        )

        flow.value = listOf(model(state = DownloadState.DOWNLOADED))
        advanceUntilIdle()

        assertEquals(
            DownloadState.DOWNLOADED,
            vm.uiState.value.rows.first { it.variant == ModelVariant.GEMMA3_1B }.downloadState
        )
    }

    @Test
    fun `GEMMA4 variant is marked as unavailable`() = runTest {
        every { observeModels() } returns flowOf(emptyList())
        val vm = viewModel()

        vm.uiState.test {
            val row = awaitItem().rows.first { it.variant == ModelVariant.GEMMA4_2B }
            assertTrue(!row.isAvailable)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
