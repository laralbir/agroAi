package com.laralnet.agroai.ui.plantation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.ui.screens.plantation.detail.PlantationDetailScreen
import com.laralnet.agroai.ui.screens.plantation.detail.PlantationDetailViewModel
import com.laralnet.agroai.ui.theme.AgroAITheme
import com.laralnet.agroai.weather.domain.model.CurrentWeather
import com.laralnet.agroai.weather.domain.model.DailyForecast
import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.domain.model.WeatherData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class PlantationDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val plantation = Plantation.create(
        name = "Test Plantation",
        type = PlantationType.OLIVAR,
        location = Location(latitude = 37.8, longitude = -5.0, municipality = "Jaén"),
        areaSqMeters = 500.0
    )

    private fun fakeForecast(): List<DailyForecast> = (0 until 15).map { i ->
        DailyForecast(
            date = Instant.now().plus(i.toLong(), ChronoUnit.DAYS),
            maxTempCelsius = 25.0 + i,
            minTempCelsius = 12.0 + i,
            precipitationProbability = if (i % 3 == 0) 40 else 0,
            precipitationMm = 0.0,
            condition = WeatherCondition.PARTLY_CLOUDY,
            windMaxKmh = 15.0,
            uvIndex = 5
        )
    }

    private fun fakeWeather(withForecast: Boolean = true) = WeatherData(
        latitude = 37.8,
        longitude = -5.0,
        current = CurrentWeather(
            temperatureCelsius = 22.0,
            humidity = 55,
            windSpeedKmh = 10.0,
            windDirection = "NE",
            precipitationMm = 0.0,
            condition = WeatherCondition.CLEAR,
            feelsLikeCelsius = 21.0
        ),
        forecast = if (withForecast) fakeForecast() else emptyList()
    )

    private fun fakeViewModel(weather: WeatherData? = fakeWeather()): PlantationDetailViewModel {
        val testPlantation: com.laralnet.agroai.plantation.domain.model.Plantation? = plantation
        return mockk(relaxed = true) {
            every { this@mockk.plantation } returns MutableStateFlow(testPlantation)
            every { this@mockk.treatments } returns MutableStateFlow<List<Treatment>>(emptyList())
            every { this@mockk.weather } returns MutableStateFlow(weather)
            every { this@mockk.actions } returns MutableStateFlow<List<PlantationAction>>(emptyList())
        }
    }

    private fun setContent(viewModel: PlantationDetailViewModel = fakeViewModel()) {
        composeRule.setContent {
            AgroAITheme {
                PlantationDetailScreen(
                    plantationId = plantation.id,
                    onNavigateBack = {},
                    onNavigateToAnalysis = {},
                    onNavigateToAnalysisWithPlant = { _, _ -> },
                    onNavigateToPlantReports = { _, _ -> },
                    onNavigateToEdit = {},
                    onNavigateToScheduleTreatment = {},
                    onNavigateToTreatmentDetail = {},
                    onNavigateToActionList = {},
                    onNavigateToActionDetail = {},
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun `plantation name is shown in the title`() {
        setContent()
        composeRule.onNodeWithText("Test Plantation").assertIsDisplayed()
    }

    @Test
    fun `forecast section card is visible when weather has forecast data`() {
        setContent()
        composeRule.onNodeWithTag("forecast_section").assertIsDisplayed()
    }

    @Test
    fun `forecast section is not visible when weather has no forecast`() {
        setContent(fakeViewModel(weather = fakeWeather(withForecast = false)))
        composeRule.onNodeWithTag("forecast_section").assertDoesNotExist()
    }

    @Test
    fun `forecast section is not visible when weather is null`() {
        setContent(fakeViewModel(weather = null))
        composeRule.onNodeWithTag("forecast_section").assertDoesNotExist()
    }

    @Test
    fun `expanding forecast section shows day rows`() {
        setContent()
        // Tap the header row to expand
        composeRule.onNodeWithTag("forecast_section").performClick()
        // First day entry should appear (emoji + temp range row)
        composeRule.onNodeWithText("25° / 12°", substring = true).assertIsDisplayed()
    }
}
