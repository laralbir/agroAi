package com.laralnet.agroai.ui.action

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.laralnet.agroai.action.application.handler.DeleteActionHandler
import com.laralnet.agroai.action.application.handler.ScheduleActionHandler
import com.laralnet.agroai.action.application.query.ObserveActionsByPlantationQuery
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.ui.screens.action.ActionFilter
import com.laralnet.agroai.ui.screens.action.ActionListViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coVerify
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

class ActionListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val actionRepository: PlantationActionRepository = mockk(relaxed = true)
    private val scheduleActionHandler: ScheduleActionHandler = mockk(relaxed = true)
    private val deleteActionHandler: DeleteActionHandler = mockk(relaxed = true)
    private val dataStore: DataStore<Preferences> = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { dataStore.data } returns flowOf(emptyPreferences())
        every { actionRepository.observeByPlantation(any()) } returns flowOf(emptyList())
    }

    private fun viewModel() = ActionListViewModel(
        observeActionsQuery = ObserveActionsByPlantationQuery(actionRepository),
        scheduleActionHandler = scheduleActionHandler,
        deleteActionHandler = deleteActionHandler,
        dataStore = dataStore
    )

    private fun action(
        id: String,
        status: ActionStatus = ActionStatus.PENDING
    ) = PlantationAction(
        id = id,
        plantationId = "p1",
        actionType = ActionType.REGAR,
        title = "Water $id",
        scheduledAt = Instant.now(),
        status = status,
        source = ActionSource.MANUAL
    )

    @Test
    fun `initial filter is ALL`() = runTest {
        val vm = viewModel()
        assertEquals(ActionFilter.ALL, vm.uiState.value.filter)
    }

    @Test
    fun `setFilter updates filter in uiState`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.setFilter(ActionFilter.PENDING)
        advanceUntilIdle()

        assertEquals(ActionFilter.PENDING, vm.uiState.value.filter)
    }

    @Test
    fun `load triggers observation of actions for the plantation`() = runTest {
        val actions = listOf(action("a1"), action("a2"))
        every { actionRepository.observeByPlantation("p1") } returns flowOf(actions)

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.actions.size)
    }

    @Test
    fun `filter PENDING shows only pending actions`() = runTest {
        val pending = action("a1", ActionStatus.PENDING)
        val done = action("a2", ActionStatus.DONE)
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(pending, done))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        vm.setFilter(ActionFilter.PENDING)
        advanceUntilIdle()

        val result = vm.uiState.value.actions
        assertEquals(1, result.size)
        assertEquals("a1", result[0].id)
    }

    @Test
    fun `filter DONE shows only done actions`() = runTest {
        val pending = action("a1", ActionStatus.PENDING)
        val done = action("a2", ActionStatus.DONE)
        every { actionRepository.observeByPlantation("p1") } returns flowOf(listOf(pending, done))

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        vm.setFilter(ActionFilter.DONE)
        advanceUntilIdle()

        val result = vm.uiState.value.actions
        assertEquals(1, result.size)
        assertEquals("a2", result[0].id)
    }

    @Test
    fun `filter ALL shows all actions regardless of status`() = runTest {
        val actions = listOf(
            action("a1", ActionStatus.PENDING),
            action("a2", ActionStatus.DONE),
            action("a3", ActionStatus.SKIPPED)
        )
        every { actionRepository.observeByPlantation("p1") } returns flowOf(actions)

        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load("p1")
        vm.setFilter(ActionFilter.ALL)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.actions.size)
    }

    @Test
    fun `actions list is empty before load is called`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.actions.isEmpty())
    }

    @Test
    fun `deleteAction delegates to DeleteActionHandler`() = runTest {
        val vm = viewModel()
        vm.deleteAction("action-99")
        advanceUntilIdle()

        coVerify { deleteActionHandler.handle(match { it.id == "action-99" }) }
    }

    @Test
    fun `scheduleAction delegates to ScheduleActionHandler`() = runTest {
        val vm = viewModel()
        vm.load("p1")
        vm.scheduleAction(ActionType.PODAR, "Prune roses", "Be careful", Instant.now())
        advanceUntilIdle()

        coVerify {
            scheduleActionHandler.handle(match {
                it.actionType == ActionType.PODAR && it.title == "Prune roses" && it.plantationId == "p1"
            })
        }
    }
}
