package com.laralnet.agroai.ui.treatment

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.ui.screens.treatment.TreatmentDetailContent
import com.laralnet.agroai.ui.screens.treatment.TreatmentDetailState
import com.laralnet.agroai.ui.theme.AgroAITheme
import com.laralnet.agroai.weather.domain.model.AlertSeverity
import com.laralnet.agroai.weather.domain.model.WeatherAlert
import com.laralnet.agroai.weather.domain.model.WeatherAlertType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TreatmentDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: TreatmentDetailState) {
        composeRule.setContent {
            AgroAITheme {
                TreatmentDetailContent(
                    state = state,
                    onNavigateBack = {},
                    onComplete = {},
                    onDelete = {}
                )
            }
        }
    }

    @Test
    fun `shows loading indicator when treatment is null`() {
        setContent(TreatmentDetailState(treatment = null, isLoading = true))
        composeRule.onNodeWithTag("treatment_loading").assertIsDisplayed()
    }

    @Test
    fun `shows treatment title when loaded`() {
        setContent(TreatmentDetailState(treatment = fakeTreatment(title = "Riego semanal")))
        composeRule.onNodeWithText("Riego semanal").assertIsDisplayed()
    }

    @Test
    fun `shows treatment info card when treatment is loaded`() {
        setContent(TreatmentDetailState(treatment = fakeTreatment()))
        composeRule.onNodeWithTag("treatment_info_card").assertIsDisplayed()
    }

    @Test
    fun `shows complete button when status is PENDING`() {
        setContent(TreatmentDetailState(treatment = fakeTreatment(status = TreatmentStatus.PENDING)))
        composeRule.onNodeWithTag("treatment_complete_btn").assertIsDisplayed()
    }

    @Test
    fun `hides complete button when status is DONE`() {
        setContent(TreatmentDetailState(treatment = fakeTreatment(status = TreatmentStatus.DONE)))
        composeRule.onNodeWithTag("treatment_complete_btn").assertDoesNotExist()
    }

    @Test
    fun `shows weather alert when alerts present`() {
        val alert = fakeAlert(WeatherAlertType.FROST)
        setContent(
            TreatmentDetailState(
                treatment = fakeTreatment(),
                weatherAlerts = listOf(alert)
            )
        )
        composeRule.onNodeWithTag("treatment_weather_alert").assertIsDisplayed()
    }

    @Test
    fun `no weather alert when alerts list is empty`() {
        setContent(
            TreatmentDetailState(
                treatment = fakeTreatment(),
                weatherAlerts = emptyList()
            )
        )
        composeRule.onNodeWithTag("treatment_weather_alert").assertDoesNotExist()
    }

    @Test
    fun `shows calendar badge when calendarEventId is set`() {
        setContent(
            TreatmentDetailState(
                treatment = fakeTreatment(calendarEventId = 999L, calendarEmail = "user@gmail.com")
            )
        )
        composeRule.onNodeWithTag("treatment_calendar_badge").assertIsDisplayed()
    }

    @Test
    fun `hides calendar badge when calendarEventId is null`() {
        setContent(TreatmentDetailState(treatment = fakeTreatment(calendarEventId = null)))
        composeRule.onNodeWithTag("treatment_calendar_badge").assertDoesNotExist()
    }

    @Test
    fun `complete button click opens dialog`() {
        setContent(TreatmentDetailState(treatment = fakeTreatment(status = TreatmentStatus.PENDING)))
        composeRule.onNodeWithTag("treatment_complete_btn").performClick()
        // Dialog title appears
        composeRule.onNodeWithText("Mark as done").assertIsDisplayed()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun fakeTreatment(
        title: String = "Test Treatment",
        status: TreatmentStatus = TreatmentStatus.PENDING,
        calendarEventId: Long? = null,
        calendarEmail: String? = null
    ) = Treatment(
        plantationId = "plant-1",
        type = TreatmentType.RIEGO,
        title = title,
        description = "Test description",
        scheduledAt = Instant.now(),
        status = status,
        calendarEventId = calendarEventId,
        calendarAccountEmail = calendarEmail
    )

    private fun fakeAlert(type: WeatherAlertType) = WeatherAlert(
        id = "alert-1",
        type = type,
        severity = AlertSeverity.HIGH,
        startAt = Instant.now(),
        endAt = Instant.now().plusSeconds(3600),
        description = "Weather warning"
    )
}
