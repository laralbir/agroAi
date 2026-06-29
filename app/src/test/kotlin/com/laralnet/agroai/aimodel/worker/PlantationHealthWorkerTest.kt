package com.laralnet.agroai.aimodel.worker

import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.aimodel.infrastructure.worker.PlantationHealthWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlantationHealthWorkerTest {

    // ── mapToActionType ────────────────────────────────────────────────────────

    @Test
    fun `mapToActionType exact ActionType name REGAR returns REGAR`() {
        assertEquals(ActionType.REGAR, PlantationHealthWorker.mapToActionType("REGAR"))
    }

    @Test
    fun `mapToActionType is case-insensitive`() {
        assertEquals(ActionType.PODAR, PlantationHealthWorker.mapToActionType("podar"))
        assertEquals(ActionType.COSECHAR, PlantationHealthWorker.mapToActionType("Cosechar"))
    }

    @Test
    fun `mapToActionType legacy RIEGO maps to REGAR`() {
        assertEquals(ActionType.REGAR, PlantationHealthWorker.mapToActionType("RIEGO"))
    }

    @Test
    fun `mapToActionType legacy PODA maps to PODAR`() {
        assertEquals(ActionType.PODAR, PlantationHealthWorker.mapToActionType("PODA"))
    }

    @Test
    fun `mapToActionType legacy COSECHA maps to COSECHAR`() {
        assertEquals(ActionType.COSECHAR, PlantationHealthWorker.mapToActionType("COSECHA"))
    }

    @Test
    fun `mapToActionType legacy FERTILIZACION maps to FERTILIZAR`() {
        assertEquals(ActionType.FERTILIZAR, PlantationHealthWorker.mapToActionType("FERTILIZACION"))
    }

    @Test
    fun `mapToActionType legacy FUMIGACION maps to FUMIGAR`() {
        assertEquals(ActionType.FUMIGAR, PlantationHealthWorker.mapToActionType("FUMIGACION"))
    }

    @Test
    fun `mapToActionType legacy INJERTO maps to INJERTAR`() {
        assertEquals(ActionType.INJERTAR, PlantationHealthWorker.mapToActionType("INJERTO"))
    }

    @Test
    fun `mapToActionType legacy TRANSPLANTE maps to TRASPLANTAR`() {
        assertEquals(ActionType.TRASPLANTAR, PlantationHealthWorker.mapToActionType("TRANSPLANTE"))
    }

    @Test
    fun `mapToActionType unknown string maps to OTRO`() {
        assertEquals(ActionType.OTRO, PlantationHealthWorker.mapToActionType("UNKNOWN_TYPE"))
        assertEquals(ActionType.OTRO, PlantationHealthWorker.mapToActionType(""))
    }

    // ── parseSuggestedDate ─────────────────────────────────────────────────────

    @Test
    fun `parseSuggestedDate null defaults to today plus 3 days at 09h00`() {
        val result = PlantationHealthWorker.parseSuggestedDate(null)
        val zone = ZoneId.systemDefault()
        val expected = LocalDate.now(zone).plusDays(3).atTime(9, 0).atZone(zone).toInstant()
        // Compare truncated to seconds to avoid nanosecond drift
        assertEquals(expected.epochSecond, result.epochSecond)
    }

    @Test
    fun `parseSuggestedDate valid future ISO date is parsed`() {
        val zone = ZoneId.systemDefault()
        val futureDate = LocalDate.now(zone).plusDays(7)
        val raw = futureDate.toString() // "YYYY-MM-DD"
        val result = PlantationHealthWorker.parseSuggestedDate(raw)
        val expected = futureDate.atTime(9, 0).atZone(zone).toInstant()
        assertEquals(expected.epochSecond, result.epochSecond)
    }

    @Test
    fun `parseSuggestedDate past date falls back to today plus 3 days`() {
        val zone = ZoneId.systemDefault()
        val pastDate = LocalDate.now(zone).minusDays(1).toString()
        val result = PlantationHealthWorker.parseSuggestedDate(pastDate)
        val expected = LocalDate.now(zone).plusDays(3).atTime(9, 0).atZone(zone).toInstant()
        assertEquals(expected.epochSecond, result.epochSecond)
    }

    @Test
    fun `parseSuggestedDate invalid string falls back to today plus 3 days`() {
        val zone = ZoneId.systemDefault()
        val result = PlantationHealthWorker.parseSuggestedDate("not-a-date")
        val expected = LocalDate.now(zone).plusDays(3).atTime(9, 0).atZone(zone).toInstant()
        assertEquals(expected.epochSecond, result.epochSecond)
    }

    @Test
    fun `parseSuggestedDate result is always in the future`() {
        val past = LocalDate.now(ZoneId.systemDefault()).minusDays(5).toString()
        val result = PlantationHealthWorker.parseSuggestedDate(past)
        assertFalse("Parsed date should be in the future", result.isBefore(Instant.now()))
    }
}
