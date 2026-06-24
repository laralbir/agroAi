package com.laralnet.agroai.ui.plantation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardState
import com.laralnet.agroai.ui.theme.AgroAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantationWizardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setWizardContent(
        state: PlantationWizardState = PlantationWizardState(),
        currentStep: Int = 0,
        onNameChange: (String) -> Unit = {},
        onTypeChange: (PlantationType) -> Unit = {},
        onNext: () -> Unit = {},
        onBack: () -> Unit = {},
        onOpenMapPicker: () -> Unit = {},
        onFinish: () -> Unit = {}
    ) {
        composeRule.setContent {
            AgroAITheme {
                PlantationWizardScreen(
                    state = state,
                    currentStep = currentStep,
                    onNameChange = onNameChange,
                    onTypeChange = onTypeChange,
                    onAreaChange = {},
                    onNotesChange = {},
                    onNext = onNext,
                    onBack = onBack,
                    onOpenMapPicker = onOpenMapPicker,
                    onMunicipalityChange = {},
                    onProvinceChange = {},
                    onMunicipalityCodeChange = {},
                    onAddPlant = {},
                    onRemovePlant = {},
                    onUpdatePlant = {},
                    onFinish = onFinish
                )
            }
        }
    }

    @Test
    fun wizardStep0_nameField_isDisplayed() {
        setWizardContent(currentStep = 0)
        composeRule.onNodeWithTag("wizard_name_field").assertIsDisplayed()
    }

    @Test
    fun wizardStep0_withEmptyName_nextButtonIsDisabled() {
        setWizardContent(state = PlantationWizardState(name = ""), currentStep = 0)
        composeRule.onNodeWithTag("wizard_next_button").assertIsNotEnabled()
    }

    @Test
    fun wizardStep0_withName_nextButtonIsEnabled() {
        setWizardContent(state = PlantationWizardState(name = "Mi plantación"), currentStep = 0)
        composeRule.onNodeWithTag("wizard_next_button").assertIsEnabled()
    }

    @Test
    fun wizardStep0_nextButtonClick_callsOnNext() {
        var clicked = false
        setWizardContent(
            state = PlantationWizardState(name = "Mi plantación"),
            currentStep = 0,
            onNext = { clicked = true }
        )
        composeRule.onNodeWithTag("wizard_next_button").performClick()
        assert(clicked)
    }

    @Test
    fun wizardStep1_typeSelector_isDisplayed() {
        setWizardContent(currentStep = 1)
        composeRule.onNodeWithTag("wizard_type_selector").assertIsDisplayed()
    }

    @Test
    fun wizardStep2_mapPickerButton_isDisplayed() {
        setWizardContent(currentStep = 2)
        composeRule.onNodeWithTag("wizard_map_picker_button").assertIsDisplayed()
    }

    @Test
    fun wizardStep2_mapPickerButton_callsOnOpenMapPicker() {
        var opened = false
        setWizardContent(currentStep = 2, onOpenMapPicker = { opened = true })
        composeRule.onNodeWithTag("wizard_map_picker_button").performClick()
        assert(opened)
    }

    @Test
    fun wizardStep2_withLocationSet_showsLocationSummary() {
        val state = PlantationWizardState(
            municipality = "Sevilla",
            province = "Sevilla",
            latitude = 37.38,
            longitude = -5.97
        )
        setWizardContent(state = state, currentStep = 2)
        composeRule.onNodeWithTag("wizard_location_summary").assertIsDisplayed()
    }

    @Test
    fun backButton_isDisplayedFromStep1() {
        setWizardContent(currentStep = 1)
        composeRule.onNodeWithTag("wizard_back_button").assertIsDisplayed()
    }
}
