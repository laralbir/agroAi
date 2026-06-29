package com.laralnet.agroai.ui.action

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.ui.screens.action.ActionFilter
import com.laralnet.agroai.ui.screens.action.ActionListScreen
import com.laralnet.agroai.ui.screens.action.ActionListUiState
import com.laralnet.agroai.ui.screens.action.ActionListViewModel
import com.laralnet.agroai.ui.theme.AgroAITheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ActionListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun action(
        id: String,
        title: String = "Water plants",
        status: ActionStatus = ActionStatus.PENDING
    ) = PlantationAction(
        id = id,
        plantationId = "p1",
        actionType = ActionType.REGAR,
        title = title,
        scheduledAt = Instant.now(),
        status = status,
        source = ActionSource.MANUAL
    )

    private fun fakeViewModel(state: ActionListUiState = ActionListUiState()): ActionListViewModel =
        mockk(relaxed = true) {
            every { this@mockk.uiState } returns MutableStateFlow(state)
        }

    private fun setContent(viewModel: ActionListViewModel = fakeViewModel()) {
        composeRule.setContent {
            AgroAITheme {
                ActionListScreen(
                    plantationId = "p1",
                    onNavigateBack = {},
                    onNavigateToDetail = {},
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun `screen title is shown`() {
        setContent()
        composeRule.onNodeWithText("Actions").assertIsDisplayed()
    }

    @Test
    fun `empty state message shown when there are no actions`() {
        setContent(fakeViewModel(ActionListUiState(actions = emptyList())))
        composeRule.onNodeWithText("No actions yet. Tap + to add one.").assertIsDisplayed()
    }

    @Test
    fun `action cards are shown when there are actions`() {
        val state = ActionListUiState(
            actions = listOf(
                action("a1", "Water the tomatoes"),
                action("a2", "Prune the roses")
            )
        )
        setContent(fakeViewModel(state))

        composeRule.onNodeWithText("Water the tomatoes").assertIsDisplayed()
        composeRule.onNodeWithText("Prune the roses").assertIsDisplayed()
    }

    @Test
    fun `filter chip All is selected by default`() {
        setContent()
        composeRule.onNodeWithText("All").assertIsSelected()
    }

    @Test
    fun `tapping Pending filter chip calls setFilter`() {
        val vm = fakeViewModel()
        setContent(vm)

        composeRule.onNodeWithText("Pending").performClick()

        verify { vm.setFilter(ActionFilter.PENDING) }
    }

    @Test
    fun `tapping Done filter chip calls setFilter with DONE`() {
        val vm = fakeViewModel()
        setContent(vm)

        composeRule.onNodeWithText("Done").performClick()

        verify { vm.setFilter(ActionFilter.DONE) }
    }

    @Test
    fun `tapping an action card navigates to detail`() {
        var navigatedId: String? = null
        val state = ActionListUiState(actions = listOf(action("a1", "Feed the plants")))
        val vm = fakeViewModel(state)

        composeRule.setContent {
            AgroAITheme {
                ActionListScreen(
                    plantationId = "p1",
                    onNavigateBack = {},
                    onNavigateToDetail = { navigatedId = it },
                    viewModel = vm
                )
            }
        }

        composeRule.onNodeWithText("Feed the plants").performClick()

        assert(navigatedId == "a1")
    }
}
