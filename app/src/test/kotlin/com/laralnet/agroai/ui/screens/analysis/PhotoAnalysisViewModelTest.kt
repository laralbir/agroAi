package com.laralnet.agroai.ui.screens.analysis

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.gemma.GemmaInferenceEngine
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

// ── Parser unit tests ─────────────────────────────────────────────────────────

class PhotoAnalysisParserTest {

    @Test
    fun `parsePhotoAnalysisResponse parses valid flat JSON`() {
        val json = """
            {
                "species": "Solanum lycopersicum",
                "generalCondition": "Good",
                "issues": ["aphids", "leaf curl"],
                "treatments": [
                    {"type": "FUMIGACION", "description": "Apply neem oil", "urgency": "this week"}
                ]
            }
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(json)

        assertEquals("Solanum lycopersicum", result.species)
        assertEquals("Good", result.generalCondition)
        assertEquals(listOf("aphids", "leaf curl"), result.issues)
        assertEquals(1, result.suggestions.size)
        assertEquals("FUMIGACION", result.suggestions[0].type)
        assertEquals("Apply neem oil", result.suggestions[0].description)
        assertEquals("this week", result.suggestions[0].urgency)
    }

    @Test
    fun `parsePhotoAnalysisResponse parses JSON inside markdown json code fence`() {
        val response = """
            Here is my analysis:
            ```json
            {
                "species": "Prunus persica",
                "generalCondition": "Fair",
                "issues": [],
                "treatments": []
            }
            ```
            Hope this helps!
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(response)

        assertEquals("Prunus persica", result.species)
        assertEquals("Fair", result.generalCondition)
        assertTrue(result.issues.isEmpty())
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun `parsePhotoAnalysisResponse parses JSON embedded in plain prose`() {
        val response = """
            Based on my analysis, the plant shows signs of stress.
            {"species": "Olea europaea", "generalCondition": "Poor", "issues": ["root rot"], "treatments": []}
            Please treat promptly.
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(response)

        assertEquals("Olea europaea", result.species)
        assertEquals("Poor", result.generalCondition)
        assertEquals(listOf("root rot"), result.issues)
    }

    @Test
    fun `parsePhotoAnalysisResponse falls back to raw response when no JSON present`() {
        val response = "I cannot analyze this image. The photo is too blurry."

        val result = parsePhotoAnalysisResponse(response)

        assertEquals("", result.species)
        assertEquals("", result.generalCondition)
        assertEquals(1, result.suggestions.size)
        assertEquals("OTRO", result.suggestions[0].type)
        assertTrue(result.suggestions[0].description.contains("blurry"))
        assertEquals(response, result.rawResponse)
    }

    @Test
    fun `parsePhotoAnalysisResponse handles missing optional suggestedDate`() {
        val json = """
            {"species":"x","generalCondition":"ok","issues":[],
             "treatments":[{"type":"RIEGO","description":"Water","urgency":"immediate"}]}
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(json)

        assertNull(result.suggestions[0].suggestedDate)
        assertEquals("immediate", result.suggestions[0].urgency)
    }

    @Test
    fun `parsePhotoAnalysisResponse preserves suggestedDate when present`() {
        val json = """
            {"species":"x","generalCondition":"ok","issues":[],
             "treatments":[{"type":"PODA","description":"Prune","urgency":"this week","suggestedDate":"2026-07-15"}]}
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(json)

        assertEquals("2026-07-15", result.suggestions[0].suggestedDate)
    }
}

// ── ViewModel unit tests ──────────────────────────────────────────────────────

class PhotoAnalysisViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val gemmaEngine: GemmaInferenceEngine = mockk(relaxed = true)
    private val modelRepository: AIModelRepository = mockk(relaxed = true)

    private fun viewModel(plantationId: String? = null): PhotoAnalysisViewModel {
        val handle = if (plantationId != null)
            SavedStateHandle(mapOf("plantationId" to plantationId))
        else
            SavedStateHandle()
        return PhotoAnalysisViewModel(handle, gemmaEngine, modelRepository)
    }

    private fun activeModel(filePath: String = "/data/models/gemma3_1b.task") = AIModel(
        id = "m1",
        variant = ModelVariant.GEMMA3_1B,
        version = ModelVariant.GEMMA3_1B.gemmaVersion,
        downloadState = DownloadState.DOWNLOADED,
        filePath = filePath,
        isActive = true
    )

    @Test
    fun `scheduleSuggestion emits ScheduleNavEvent when plantationId is set`() = runTest {
        coEvery { modelRepository.findActive() } returns null
        val vm = viewModel(plantationId = "plant-42")
        advanceUntilIdle()

        val suggestion = TreatmentSuggestion("RIEGO", "Water regularly", "immediate", null)

        vm.scheduleEvent.test {
            vm.scheduleSuggestion(suggestion)
            val event = awaitItem()
            assertEquals("plant-42", event.plantationId)
            assertEquals(suggestion, event.suggestion)
            assertNull(event.rawAnalysis) // no analysis performed yet
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `scheduleSuggestion does nothing when plantationId is null`() = runTest {
        coEvery { modelRepository.findActive() } returns null
        val vm = viewModel(plantationId = null)
        advanceUntilIdle()

        val suggestion = TreatmentSuggestion("RIEGO", "Water", "low", null)

        vm.scheduleEvent.test {
            vm.scheduleSuggestion(suggestion)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `modelLoaded is false when no active model exists`() = runTest {
        coEvery { modelRepository.findActive() } returns null
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.modelLoaded)
    }

    @Test
    fun `modelLoaded is true when active model is already loaded in engine`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.modelLoaded)
    }

    @Test
    fun `analyzePhoto sets analysisResult and clears streamingText after completion`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        coEvery { modelRepository.findPromptTemplate(any()) } returns null
        val responseJson = """{"species":"Vitis vinifera","generalCondition":"Healthy","issues":[],"treatments":[]}"""
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } returns flowOf(responseJson)

        val vm = viewModel(plantationId = "p1")
        advanceUntilIdle()

        val uri = mockk<Uri>()
        vm.analyzePhoto(uri)
        advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isAnalyzing)
            assertNotNull(analysisResult)
            assertEquals("Vitis vinifera", analysisResult!!.species)
            assertEquals("Healthy", analysisResult!!.generalCondition)
            assertEquals("", streamingText)
            assertNull(error)
        }
    }

    @Test
    fun `analyzePhoto resets previous result before starting new analysis`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        coEvery { modelRepository.findPromptTemplate(any()) } returns null
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } returns flowOf(
            """{"species":"Citrus","generalCondition":"ok","issues":[],"treatments":[]}"""
        )

        val vm = viewModel(plantationId = "p1")
        advanceUntilIdle()

        val uri = mockk<Uri>()
        vm.analyzePhoto(uri)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.analysisResult)

        // Start a new analysis — result should be cleared while analyzing
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } returns flowOf(
            """{"species":"Olea","generalCondition":"good","issues":[],"treatments":[]}"""
        )
        vm.analyzePhoto(uri)
        // After completion, new result is set
        advanceUntilIdle()

        assertEquals("Olea", vm.uiState.value.analysisResult?.species)
    }

    @Test
    fun `analyzePhoto sets error state when engine throws`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        coEvery { modelRepository.findPromptTemplate(any()) } returns null
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } throws RuntimeException("Engine failure")

        val vm = viewModel(plantationId = "p1")
        advanceUntilIdle()

        val uri = mockk<Uri>()
        vm.analyzePhoto(uri)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isAnalyzing)
    }
}
