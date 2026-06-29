package com.laralnet.agroai.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.ui.screens.home.HomeScreen
import com.laralnet.agroai.ui.screens.home.HomeViewModel
import com.laralnet.agroai.ui.theme.AgroAITheme
import com.laralnet.agroai.weather.domain.model.WeatherData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fakeViewModel(
        plantations: List<Plantation> = emptyList(),
        hasActiveModel: Boolean = false,
        todayTreatments: List<Treatment> = emptyList(),
        upcomingTreatments: List<Treatment> = emptyList(),
        homeWeather: WeatherData? = null
    ): HomeViewModel = mockk(relaxed = true) {
        every { this@mockk.plantations } returns MutableStateFlow(plantations)
        every { this@mockk.hasActiveModel } returns MutableStateFlow(hasActiveModel)
        every { this@mockk.todayTreatments } returns MutableStateFlow(todayTreatments)
        every { this@mockk.upcomingTreatments } returns MutableStateFlow(upcomingTreatments)
        every { this@mockk.homeWeather } returns MutableStateFlow(homeWeather)
    }

    private fun setContent(viewModel: HomeViewModel = fakeViewModel()) {
        composeRule.setContent {
            AgroAITheme {
                HomeScreen(
                    onNavigateToPlantations = {},
                    onNavigateToPlantationDetail = {},
                    onNavigateToModels = {},
                    onNavigateToTreatmentDetail = {},
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun `empty state message shown when no plantations`() {
        setContent(fakeViewModel(plantations = emptyList()))
        composeRule.onNodeWithText("No plantations yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun `plantation name shown when plantations exist`() {
        val plantation = Plantation.create(
            name = "Mi Olivar",
            type = PlantationType.OLIVAR,
            location = Location(municipality = "Jaén", province = "Jaén"),
            areaSqMeters = 5000.0
        )
        setContent(fakeViewModel(plantations = listOf(plantation)))
        composeRule.onNodeWithText("Mi Olivar").assertIsDisplayed()
    }

    @Test
    fun `multiple plantation names shown`() {
        val plantations = listOf(
            Plantation.create("Huerta Norte", PlantationType.HUERTA, Location(), 100.0),
            Plantation.create("Viñedo Sur", PlantationType.VIÑEDO, Location(), 2000.0),
            Plantation.create("Olivar Este", PlantationType.OLIVAR, Location(), 3000.0)
        )
        setContent(fakeViewModel(plantations = plantations))

        composeRule.onNodeWithText("Huerta Norte").assertIsDisplayed()
        composeRule.onNodeWithText("Viñedo Sur").assertIsDisplayed()
        composeRule.onNodeWithText("Olivar Este").assertIsDisplayed()
    }

    @Test
    fun `add plantation FAB button is always shown`() {
        setContent()
        composeRule.onNodeWithText("Add Plantation").assertIsDisplayed()
    }
}
