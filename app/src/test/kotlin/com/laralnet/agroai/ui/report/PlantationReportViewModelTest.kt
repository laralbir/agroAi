package com.laralnet.agroai.ui.report

import com.laralnet.agroai.action.application.query.ObserveActionsByPlantationQuery
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.ui.screens.report.PlantationReportViewModel
import com.laralnet.agroai.ui.screens.report.ReportDateRange
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlantationReportViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val actionRepository: PlantationActionRepository = mockk(relaxed = true)
    private val plantationRepository: PlantationRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { actionRepository.observeByPlantation(any()) } returns flowOf(emptyList())
        coEvery { plantationRepository.findById(any()) } returns null
    }

    private fun viewModel() = PlantationReportViewModel(
        observeActionsQuery = ObserveActionsByPlantationQuery(actionRepository),
        plantationRepository = plantationRepository
    )

    private fun action(
        id: String,
        status: ActionStatus = ActionStatus.PENDING,
        actionType: ActionType = ActionType.REGAR,
        plantTypeId: String? = null,
        scheduledAt: Instant = Instant.now()
    ) = PlantationAction(
        id = id,
        plantationId = "p1",
        plantTypeId = plantTypeId,
        actionType = actionType,
        title = "Action $id",
        scheduledAt = scheduledAt,
        status = status,
        source = ActionSource.MANUAL
    )

    @Test
    fun `completedActions contains only DONE actions`() = runTest {
        val done = action("a1", ActionStatus.DONE)
        val pending = action("a2", ActionStatus.PENDING)
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(done, pending))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.completedActions.size)
        assertEquals("a1", state.completedActions[0].id)
    }

    @Test
    fun `pendingActions contains only PENDING actions`() = runTest {
        val done = action("a1", ActionStatus.DONE)
        val pending = action("a2", ActionStatus.PENDING)
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(done, pending))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.pendingActions.size)
        assertEquals("a2", state.pendingActions[0].id)
    }

    @Test
    fun `setActionTypeFilter filters actions by type`() = runTest {
        val regar = action("a1", ActionStatus.DONE, ActionType.REGAR)
        val podar = action("a2", ActionStatus.DONE, ActionType.PODAR)
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(regar, podar))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        vm.setActionTypeFilter(ActionType.REGAR)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.completedActions.size)
        assertEquals(ActionType.REGAR, state.completedActions[0].actionType)
    }

    @Test
    fun `clearFilters resets all filters`() = runTest {
        val regar = action("a1", ActionStatus.DONE, ActionType.REGAR)
        val podar = action("a2", ActionStatus.DONE, ActionType.PODAR)
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(regar, podar))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        vm.setActionTypeFilter(ActionType.REGAR)
        vm.clearFilters()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.completedActions.size)
    }

    @Test
    fun `setDateRangeFilter filters actions by date`() = runTest {
        val today = LocalDate.now(ZoneId.systemDefault())
        val old = action(
            "old",
            ActionStatus.DONE,
            scheduledAt = today.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant()
        )
        val recent = action(
            "recent",
            ActionStatus.DONE,
            scheduledAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(old, recent))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        vm.setDateRangeFilter(ReportDateRange(from = today.minusDays(3)))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.completedActions.size)
        assertEquals("recent", vm.uiState.value.completedActions[0].id)
    }

    @Test
    fun `exportContent is non-blank when actions exist`() = runTest {
        val done = action("a1", ActionStatus.DONE)
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(done))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.exportContent.isNotBlank())
    }

    @Test
    fun `empty plantation returns empty state`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.completedActions.isEmpty())
        assertTrue(vm.uiState.value.pendingActions.isEmpty())
    }
}
