package com.laralnet.agroai.ui.plantation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardState
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardViewModel
import com.laralnet.agroai.ui.theme.AgroAITheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantationWizardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fakeViewModel(
        state: PlantationWizardState = PlantationWizardState(),
        step: Int = 0
    ): PlantationWizardViewModel = mockk(relaxed = true) {
        every { this@mockk.uiState } returns MutableStateFlow(state)
        every { this@mockk.currentStep } returns MutableStateFlow(step)
    }

    private fun setContent(
        viewModel: PlantationWizardViewModel = fakeViewModel(),
        onOpenMapPicker: () -> Unit = {}
    ) {
        composeRule.setContent {
            AgroAITheme {
                PlantationWizardScreen(
                    onNavigateBack = {},
                    onPlantationSaved = {},
                    onOpenMapPicker = onOpenMapPicker,
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun `step 0 shows plantation name field`() {
        setContent(fakeViewModel(step = 0))
        composeRule.onNodeWithText("Plantation name").assertIsDisplayed()
    }

    @Test
    fun `step 0 shows Next button`() {
        setContent(fakeViewModel(step = 0))
        composeRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun `step 0 does not show Back button`() {
        setContent(fakeViewModel(step = 0))
        composeRule.onNodeWithText("Back").assertIsNotDisplayed()
    }

    @Test
    fun `step 1 shows Back button`() {
        setContent(fakeViewModel(step = 1))
        composeRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun `clicking Next calls viewModel nextStep`() {
        val vm = fakeViewModel(step = 0)
        setContent(vm)

        composeRule.onNodeWithText("Next").performClick()

        verify { vm.nextStep() }
    }

    @Test
    fun `clicking Back calls viewModel previousStep`() {
        val vm = fakeViewModel(step = 1)
        setContent(vm)

        composeRule.onNodeWithText("Back").performClick()

        verify { vm.previousStep() }
    }

    @Test
    fun `step 2 shows Pick on map button`() {
        setContent(fakeViewModel(step = 1))
        composeRule.onNodeWithText("Pick on map").assertIsDisplayed()
    }

    @Test
    fun `clicking map picker button calls onOpenMapPicker`() {
        var called = false
        setContent(fakeViewModel(step = 1), onOpenMapPicker = { called = true })

        composeRule.onNodeWithText("Pick on map").performClick()

        assert(called)
    }
}
