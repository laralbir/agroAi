package com.laralnet.agroai.ui.screens.analysis

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.infrastructure.gemma.GemmaInferenceEngine
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.photoanalysis.application.handler.SaveAnalysisHandler
import com.laralnet.agroai.photoanalysis.application.query.ObserveAnalysesQuery
import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.weather.application.handler.RefreshWeatherHandler
import com.laralnet.agroai.weather.application.query.ObserveWeatherQuery
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coJustRun
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
    fun `parsePhotoAnalysisResponse extracts actions from JSON block`() {
        val response = """
            ## Analysis
            The plant shows aphid infestation.
            ---
            ```json
            {"actions":[{"type":"FUMIGACION","title":"Apply neem oil","description":"Spray with diluted neem oil","urgency":"HIGH","suggestedDate":"2026-07-10"}]}
            ```
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(response)

        assertEquals(1, result.suggestions.size)
        assertEquals("FUMIGACION", result.suggestions[0].type)
        assertEquals("Apply neem oil", result.suggestions[0].title)
        assertEquals("Spray with diluted neem oil", result.suggestions[0].description)
        assertEquals("HIGH", result.suggestions[0].urgency)
        assertEquals("2026-07-10", result.suggestions[0].suggestedDate)
    }

    @Test
    fun `parsePhotoAnalysisResponse returns empty suggestions when no actions block`() {
        val response = "I cannot analyze this image. Please try again with better lighting."

        val result = parsePhotoAnalysisResponse(response)

        assertTrue(result.suggestions.isEmpty())
        assertEquals(response, result.rawResponse)
    }

    @Test
    fun `parsePhotoAnalysisResponse handles multiple actions`() {
        val response = """
            Some analysis text.
            ```json
            {"actions":[
              {"type":"RIEGO","title":"Irrigation","description":"Water deeply","urgency":"MEDIUM","suggestedDate":"2026-07-05"},
              {"type":"PODA","title":"Pruning","description":"Remove dead branches","urgency":"LOW","suggestedDate":null}
            ]}
            ```
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(response)

        assertEquals(2, result.suggestions.size)
        assertEquals("RIEGO", result.suggestions[0].type)
        assertEquals("PODA", result.suggestions[1].type)
        assertNull(result.suggestions[1].suggestedDate)
    }

    @Test
    fun `parsePhotoAnalysisResponse discards past dates and keeps null`() {
        val response = """
            ```json
            {"actions":[{"type":"RIEGO","title":"Water","description":"Irrigate","urgency":"LOW","suggestedDate":"2024-03-15"}]}
            ```
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(response)

        // 2024 is in the past — must be discarded rather than shown to the user
        assertNull(result.suggestions[0].suggestedDate)
    }

    @Test
    fun `parsePhotoAnalysisResponse keeps future dates`() {
        val response = """
            ```json
            {"actions":[{"type":"PODA","title":"Prune","description":"Cut","urgency":"LOW","suggestedDate":"2026-12-01"}]}
            ```
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(response)

        assertEquals("2026-12-01", result.suggestions[0].suggestedDate)
    }

    @Test
    fun `parsePhotoAnalysisResponse normalizes type to uppercase`() {
        val response = """
            ```json
            {"actions":[{"type":"fumigacion","title":"Spray","description":"Apply","urgency":"low"}]}
            ```
        """.trimIndent()

        val result = parsePhotoAnalysisResponse(response)

        assertEquals("FUMIGACION", result.suggestions[0].type)
    }

    @Test
    fun `parsePhotoAnalysisResponse finds actions block without markdown fence`() {
        val response = """Some text {"actions":[{"type":"OTRO","title":"Check","description":"Monitor","urgency":"LOW"}]} more text"""

        val result = parsePhotoAnalysisResponse(response)

        assertEquals(1, result.suggestions.size)
        assertEquals("OTRO", result.suggestions[0].type)
    }
}

// ── ViewModel unit tests ──────────────────────────────────────────────────────

class PhotoAnalysisViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val gemmaEngine: GemmaInferenceEngine = mockk(relaxed = true)
    private val modelRepository: AIModelRepository = mockk(relaxed = true)
    private val plantationRepository: PlantationRepository = mockk(relaxed = true)
    private val observeWeatherQuery: ObserveWeatherQuery = mockk(relaxed = true)
    private val refreshWeatherHandler: RefreshWeatherHandler = mockk(relaxed = true)
    private val saveAnalysisHandler: SaveAnalysisHandler = mockk(relaxed = true)
    private val observeAnalysesQuery: ObserveAnalysesQuery = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val sharedPrefs: SharedPreferences = mockk(relaxed = true)

    private fun viewModel(plantationId: String? = null): PhotoAnalysisViewModel {
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.getString(any(), any()) } returns "ENGLISH"
        every { plantationRepository.observeAll() } returns flowOf(emptyList())
        coEvery { observeWeatherQuery.invoke(any(), any()) } returns flowOf(null)
        coJustRun { refreshWeatherHandler.handle(any(), any()) }
        every { observeAnalysesQuery.invoke(any()) } returns flowOf(emptyList())
        coEvery { saveAnalysisHandler.handle(any()) } returns Result.success(
            AnalysisRecord(plantationId = null, plantTypeId = null, plantationName = null, plantTypeName = null, rawResponse = "")
        )

        val handle = if (plantationId != null)
            SavedStateHandle(mapOf("plantationId" to plantationId))
        else
            SavedStateHandle()
        return PhotoAnalysisViewModel(
            savedStateHandle = handle,
            context = context,
            gemmaEngine = gemmaEngine,
            modelRepository = modelRepository,
            plantationRepository = plantationRepository,
            observeWeatherQuery = observeWeatherQuery,
            refreshWeatherHandler = refreshWeatherHandler,
            saveAnalysisHandler = saveAnalysisHandler,
            observeAnalysesQuery = observeAnalysesQuery
        )
    }

    private fun activeModel() = AIModel(
        id = "m1",
        variant = ModelVariant.GEMMA3N_E2B,
        version = ModelVariant.GEMMA3N_E2B.gemmaVersion,
        downloadState = DownloadState.DOWNLOADED,
        filePath = "/data/models/gemma3_1b.task",
        isActive = true
    )

    @Test
    fun `setImageUri stores URI without triggering analysis`() = runTest {
        coEvery { modelRepository.findActive() } returns null
        val vm = viewModel()
        val uri = mockk<Uri>()

        vm.setImageUri(uri)
        advanceUntilIdle()

        assertEquals(uri, vm.uiState.value.imageUri)
        assertFalse(vm.uiState.value.isAnalyzing)
        assertNull(vm.uiState.value.analysisResult)
    }

    @Test
    fun `setImageUri clears previous analysis result`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        coEvery { modelRepository.findPromptTemplate(any()) } returns null
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } returns flowOf(
            """{"actions":[{"type":"RIEGO","title":"Water","description":"Irrigate","urgency":"LOW"}]}"""
        )

        val vm = viewModel(plantationId = "p1")
        advanceUntilIdle()

        val uri = mockk<Uri>()
        vm.setImageUri(uri)
        vm.analyzePhoto()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.analysisResult)

        // Setting a new URI should wipe the result
        vm.setImageUri(mockk())
        assertNull(vm.uiState.value.analysisResult)
    }

    @Test
    fun `analyzePhoto does nothing when no imageUri is set`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true

        val vm = viewModel()
        advanceUntilIdle()

        vm.analyzePhoto()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isAnalyzing)
        assertNull(vm.uiState.value.analysisResult)
    }

    @Test
    fun `analyzePhoto parses actions block and populates suggestions`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        coEvery { modelRepository.findPromptTemplate(any()) } returns null
        val responseWithActions = """
            ## Analysis
            The vine looks healthy overall.
            ```json
            {"actions":[{"type":"RIEGO","title":"Water weekly","description":"Deep irrigation","urgency":"LOW","suggestedDate":"2026-07-20"}]}
            ```
        """.trimIndent()
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } returns flowOf(responseWithActions)

        val vm = viewModel(plantationId = "p1")
        advanceUntilIdle()

        vm.setImageUri(mockk<Uri>())
        vm.analyzePhoto()
        advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isAnalyzing)
            assertNotNull(analysisResult)
            assertEquals(1, analysisResult!!.suggestions.size)
            assertEquals("RIEGO", analysisResult!!.suggestions[0].type)
            assertEquals("Water weekly", analysisResult!!.suggestions[0].title)
            assertEquals("2026-07-20", analysisResult!!.suggestions[0].suggestedDate)
            assertEquals("", streamingText)
            assertNull(error)
        }
    }

    @Test
    fun `analyzePhoto sets error state when engine throws`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        coEvery { modelRepository.findPromptTemplate(any()) } returns null
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } throws RuntimeException("Engine failure")

        val vm = viewModel(plantationId = "p1")
        advanceUntilIdle()

        vm.setImageUri(mockk<Uri>())
        vm.analyzePhoto()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isAnalyzing)
    }

    @Test
    fun `supportsVision is false when no model is loaded`() = runTest {
        coEvery { modelRepository.findActive() } returns null
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.supportsVision)
        assertFalse(vm.uiState.value.modelLoaded)
    }

    @Test
    fun `supportsVision reflects engine capability when model is loaded`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        every { gemmaEngine.supportsImageAnalysis() } returns true

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.modelLoaded)
        assertTrue(vm.uiState.value.supportsVision)
    }

    @Test
    fun `scheduleSuggestion emits ScheduleNavEvent when plantationId is set`() = runTest {
        coEvery { modelRepository.findActive() } returns null
        val vm = viewModel(plantationId = "plant-42")
        advanceUntilIdle()

        val suggestion = TreatmentSuggestion("RIEGO", "Water regularly", "Deep irrigation", "immediate", null)

        vm.scheduleEvent.test {
            vm.scheduleSuggestion(suggestion)
            val event = awaitItem()
            assertEquals("plant-42", event.plantationId)
            assertEquals(suggestion, event.suggestion)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `scheduleSuggestion does nothing when plantationId is null`() = runTest {
        coEvery { modelRepository.findActive() } returns null
        val vm = viewModel(plantationId = null)
        advanceUntilIdle()

        val suggestion = TreatmentSuggestion("RIEGO", "Water", "Irrigate", "low", null)

        vm.scheduleEvent.test {
            vm.scheduleSuggestion(suggestion)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `userQuestion is included in enriched prompt`() = runTest {
        coEvery { modelRepository.findActive() } returns activeModel()
        every { gemmaEngine.isModelLoaded() } returns true
        coEvery { modelRepository.findPromptTemplate(any()) } returns null

        var capturedPrompt: String? = null
        every { gemmaEngine.analyzePhoto(any(), any(), capture(capturedPrompt?.let { io.mockk.slot() } ?: io.mockk.slot<String>().also { capturedPrompt = "" })) } answers {
            flowOf("")
        }
        // Simpler: capture the prompt via every block
        every { gemmaEngine.analyzePhoto(any(), any(), any()) } answers {
            capturedPrompt = thirdArg<String>()
            flowOf("""{"actions":[]}""")
        }

        val vm = viewModel(plantationId = "p1")
        advanceUntilIdle()

        vm.setUserQuestion("Is the tomato ready to harvest?")
        vm.setImageUri(mockk<Uri>())
        vm.analyzePhoto()
        advanceUntilIdle()

        assertNotNull(capturedPrompt)
        assertTrue(
            "Expected user question in prompt but got: $capturedPrompt",
            capturedPrompt!!.contains("Is the tomato ready to harvest?")
        )
    }
}
