package com.laralnet.agroai.ui.workerlog

import com.laralnet.agroai.aimodel.domain.model.WorkerRun
import com.laralnet.agroai.aimodel.domain.repository.WorkerRunRepository
import com.laralnet.agroai.ui.screens.workerlog.WorkerLogViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class WorkerLogViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val workerRunRepository: WorkerRunRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { workerRunRepository.observeAll() } returns flowOf(emptyList())
    }

    private fun viewModel() = WorkerLogViewModel(workerRunRepository)

    private fun run(
        id: String,
        plantationId: String? = "p1",
        actionsCreated: Int = 2
    ) = WorkerRun(
        id = id,
        timestamp = Instant.now(),
        plantationId = plantationId,
        plantationName = plantationId?.let { "Plantation $it" },
        actionsCreated = actionsCreated,
        summary = "## Summary\n$actionsCreated actions created",
        durationMs = 1500L
    )

    @Test
    fun `empty list when no runs`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.runs.isEmpty())
    }

    @Test
    fun `runs list reflects repository`() = runTest {
        every { workerRunRepository.observeAll() } returns flowOf(listOf(run("r1"), run("r2")))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.runs.size)
    }

    @Test
    fun `loadDetail populates detailState`() = runTest {
        val run = run("r1")
        coEvery { workerRunRepository.findById("r1") } returns run

        val vm = viewModel()
        vm.loadDetail("r1")
        advanceUntilIdle()

        assertEquals("r1", vm.detailState.value.run?.id)
    }

    @Test
    fun `loadDetail with unknown id leaves run null`() = runTest {
        coEvery { workerRunRepository.findById("unknown") } returns null

        val vm = viewModel()
        vm.loadDetail("unknown")
        advanceUntilIdle()

        assertNull(vm.detailState.value.run)
    }

    @Test
    fun `isLoading is false after runs load`() = runTest {
        every { workerRunRepository.observeAll() } returns flowOf(listOf(run("r1")))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(!vm.uiState.value.isLoading)
    }
}
