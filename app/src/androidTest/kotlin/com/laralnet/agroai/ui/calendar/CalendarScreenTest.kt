package com.laralnet.agroai.ui.calendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.ui.screens.calendar.CalendarContent
import com.laralnet.agroai.ui.screens.calendar.CalendarUiState
import com.laralnet.agroai.ui.theme.AgroAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testMonth = YearMonth.of(2026, 6)
    private val testDay = LocalDate.of(2026, 6, 15)

    private fun setContent(
        uiState: CalendarUiState = CalendarUiState(currentMonth = testMonth, selectedDay = testDay),
        hasPermission: Boolean = true,
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
        onDaySelected: (LocalDate) -> Unit = {},
        onGrantPermission: () -> Unit = {}
    ) {
        composeRule.setContent {
            AgroAITheme {
                CalendarContent(
                    uiState = uiState,
                    hasPermission = hasPermission,
                    onNavigateToTreatmentDetail = {},
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onDaySelected = onDaySelected,
                    onGrantPermission = onGrantPermission
                )
            }
        }
    }

    @Test
    fun `shows permission screen when permission not granted`() {
        setContent(hasPermission = false)
        composeRule.onNodeWithTag("calendar_permission_screen").assertIsDisplayed()
    }

    @Test
    fun `hides permission screen when permission granted`() {
        setContent(hasPermission = true)
        composeRule.onNodeWithTag("calendar_permission_screen").assertDoesNotExist()
    }

    @Test
    fun `shows calendar content when permission granted`() {
        setContent(hasPermission = true)
        composeRule.onNodeWithTag("calendar_content").assertIsDisplayed()
    }

    @Test
    fun `grant permission button triggers callback`() {
        var granted = false
        setContent(hasPermission = false, onGrantPermission = { granted = true })
        composeRule.onNodeWithText("Grant Permission").performClick()
        assert(granted)
    }

    @Test
    fun `previous month button triggers callback`() {
        var clicked = false
        setContent(onPrevious = { clicked = true })
        composeRule
            .onNodeWithContentDescription("Previous month")
            .performClick()
        assert(clicked)
    }

    @Test
    fun `next month button triggers callback`() {
        var clicked = false
        setContent(onNext = { clicked = true })
        composeRule
            .onNodeWithContentDescription("Next month")
            .performClick()
        assert(clicked)
    }

    @Test
    fun `shows current month name in header`() {
        setContent(
            uiState = CalendarUiState(
                currentMonth = YearMonth.of(2026, 6),
                selectedDay = LocalDate.of(2026, 6, 1)
            )
        )
        // Header shows month name — "June 2026" in English locale
        composeRule.onNodeWithText("June 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun `shows empty day list message when no treatments`() {
        setContent(
            uiState = CalendarUiState(
                currentMonth = testMonth,
                selectedDay = testDay,
                selectedDayTreatments = emptyList()
            )
        )
        composeRule
            .onNodeWithText("No treatments scheduled for this day")
            .assertIsDisplayed()
    }
}
