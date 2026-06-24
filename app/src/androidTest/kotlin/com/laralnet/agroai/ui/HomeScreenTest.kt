package com.laralnet.agroai.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.ui.screens.home.HomeScreen
import com.laralnet.agroai.ui.screens.home.HomeUiState
import com.laralnet.agroai.ui.theme.AgroAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setHomeContent(state: HomeUiState) {
        composeRule.setContent {
            AgroAITheme {
                HomeScreen(
                    uiState = state,
                    onNavigateToPlantation = {},
                    onAddPlantation = {}
                )
            }
        }
    }

    @Test
    fun homeScreen_displaysEmptyState_whenNoPlantations() {
        setHomeContent(HomeUiState(plantations = emptyList(), isLoading = false))

        composeRule.onNodeWithTag("home_empty_state").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysLoadingIndicator_whenLoading() {
        setHomeContent(HomeUiState(plantations = emptyList(), isLoading = true))

        composeRule.onNodeWithTag("home_loading").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysPlantationCard_whenListNotEmpty() {
        val plantation = Plantation.create(
            name = "Mi Olivar",
            type = PlantationType.OLIVAR,
            location = Location(municipality = "Jaén", province = "Jaén"),
            areaSqMeters = 5000.0
        )
        setHomeContent(HomeUiState(plantations = listOf(plantation), isLoading = false))

        composeRule.onNodeWithText("Mi Olivar").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysMultiplePlantations() {
        val plantations = listOf(
            Plantation.create("Huerta Norte", PlantationType.HUERTA, Location(), 100.0),
            Plantation.create("Viñedo Sur", PlantationType.VIÑEDO, Location(), 2000.0),
            Plantation.create("Olivar Este", PlantationType.OLIVAR, Location(), 3000.0)
        )
        setHomeContent(HomeUiState(plantations = plantations, isLoading = false))

        composeRule.onNodeWithText("Huerta Norte").assertIsDisplayed()
        composeRule.onNodeWithText("Viñedo Sur").assertIsDisplayed()
        composeRule.onNodeWithText("Olivar Este").assertIsDisplayed()
    }
}
