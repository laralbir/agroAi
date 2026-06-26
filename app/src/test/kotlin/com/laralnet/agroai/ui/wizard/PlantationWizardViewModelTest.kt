package com.laralnet.agroai.ui.wizard

import androidx.lifecycle.SavedStateHandle
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimApiService
import com.laralnet.agroai.plantation.application.handler.CreatePlantationHandler
import com.laralnet.agroai.plantation.application.handler.UpdatePlantationHandler
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlantationWizardViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val createHandler: CreatePlantationHandler = mockk()
    private val updateHandler: UpdatePlantationHandler = mockk(relaxed = true)
    private val repository: PlantationRepository = mockk(relaxed = true)
    private val nominatim: NominatimApiService = mockk(relaxed = true)
    private val savedStateHandle = SavedStateHandle(emptyMap())

    private val viewModel = PlantationWizardViewModel(
        savedStateHandle = savedStateHandle,
        createPlantationHandler = createHandler,
        updatePlantationHandler = updateHandler,
        plantationRepository = repository,
        nominatim = nominatim
    )

    @Test
    fun `initial state has empty name and step 0`() {
        assertEquals("", viewModel.uiState.value.name)
        assertEquals(0, viewModel.currentStep.value)
    }

    @Test
    fun `setName updates name in state`() {
        viewModel.setName("Mi plantación")
        assertEquals("Mi plantación", viewModel.uiState.value.name)
    }

    @Test
    fun `setType updates type in state`() {
        viewModel.setType(PlantationType.OLIVAR)
        assertEquals(PlantationType.OLIVAR, viewModel.uiState.value.type)
    }

    @Test
    fun `nextStep increments currentStep`() {
        viewModel.nextStep()
        assertEquals(1, viewModel.currentStep.value)
    }

    @Test
    fun `previousStep decrements currentStep and does not go below 0`() {
        viewModel.nextStep()
        viewModel.previousStep()
        assertEquals(0, viewModel.currentStep.value)

        viewModel.previousStep()
        assertEquals(0, viewModel.currentStep.value)
    }

    @Test
    fun `addPlant adds a PlantForm to the list`() {
        assertTrue(viewModel.uiState.value.plantForms.isEmpty())
        viewModel.addPlant()
        assertEquals(1, viewModel.uiState.value.plantForms.size)
    }

    @Test
    fun `removePlant removes the correct plant`() {
        viewModel.addPlant()
        viewModel.addPlant()
        val idToRemove = viewModel.uiState.value.plantForms.first().id
        viewModel.removePlant(0)
        assertEquals(1, viewModel.uiState.value.plantForms.size)
        assertTrue(viewModel.uiState.value.plantForms.none { it.id == idToRemove })
    }

    @Test
    fun `setLocationFromMap populates location fields`() {
        val location = Location(
            latitude = 40.0,
            longitude = -3.5,
            municipality = "Toledo",
            province = "Toledo"
        )
        viewModel.setLocationFromMap(location)

        val state = viewModel.uiState.value
        assertEquals(40.0, state.latitude!!, 0.001)
        assertEquals(-3.5, state.longitude!!, 0.001)
        assertEquals("Toledo", state.municipality)
    }

    @Test
    fun `save() sets savedId on success`() = runTest {
        val fakePlantation = Plantation.create(
            name = "Test",
            type = PlantationType.HUERTA,
            location = Location(),
            areaSqMeters = 100.0
        )
        coEvery { createHandler.handle(any()) } returns Result.success(fakePlantation)

        viewModel.setName("Test")
        viewModel.save()

        assertEquals(fakePlantation.id, viewModel.uiState.value.savedId)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `save() sets error on failure`() = runTest {
        coEvery { createHandler.handle(any()) } returns Result.failure(RuntimeException("DB error"))

        viewModel.save()

        assertNotNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.savedId)
    }

    // ── Edit mode ─────────────────────────────────────────────────────────────

    private fun editViewModel(existingId: String): PlantationWizardViewModel =
        PlantationWizardViewModel(
            savedStateHandle = SavedStateHandle(mapOf("id" to existingId)),
            createPlantationHandler = createHandler,
            updatePlantationHandler = updateHandler,
            plantationRepository = repository,
            nominatim = nominatim
        )

    @Test
    fun `edit mode is detected when SavedStateHandle contains id`() = runTest {
        val plantation = plantation("p-42", "Olivar Norte")
        coEvery { repository.findById("p-42") } returns plantation

        val vm = editViewModel("p-42")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEditMode)
    }

    @Test
    fun `loadForEdit populates state with existing plantation data`() = runTest {
        val plantation = plantation("p-42", "Olivar Norte", PlantationType.OLIVAR, areaSqMeters = 3000.0)
        coEvery { repository.findById("p-42") } returns plantation

        val vm = editViewModel("p-42")
        advanceUntilIdle()

        assertEquals("Olivar Norte", vm.uiState.value.name)
        assertEquals(PlantationType.OLIVAR, vm.uiState.value.type)
        assertEquals("3000", vm.uiState.value.areaSqMeters)
    }

    @Test
    fun `loadForEdit populates plants from existing plantation`() = runTest {
        val plant = PlantType(plantationId = "p-42", name = "Arbequina", variety = "Clásica", count = 150)
        val plantation = plantation("p-42", "Olivar Norte", plants = listOf(plant))
        coEvery { repository.findById("p-42") } returns plantation

        val vm = editViewModel("p-42")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.plantForms.size)
        assertEquals("Arbequina", vm.uiState.value.plantForms.first().name)
        assertEquals("150", vm.uiState.value.plantForms.first().count)
    }

    @Test
    fun `save() in edit mode calls updatePlantationHandler`() = runTest {
        val plantation = plantation("p-42", "Olivar Norte")
        coEvery { repository.findById("p-42") } returns plantation
        coEvery { updateHandler.handle(any()) } returns Result.success(Unit)

        val vm = editViewModel("p-42")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()

        coVerify { updateHandler.handle(any()) }
        coVerify(exactly = 0) { createHandler.handle(any()) }
    }

    @Test
    fun `save() in edit mode sets savedId to the existing plantation id`() = runTest {
        val plantation = plantation("p-42", "Olivar Norte")
        coEvery { repository.findById("p-42") } returns plantation
        coEvery { updateHandler.handle(any()) } returns Result.success(Unit)

        val vm = editViewModel("p-42")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()

        assertEquals("p-42", vm.uiState.value.savedId)
    }

    @Test
    fun `save() in edit mode sets error when update fails`() = runTest {
        val plantation = plantation("p-42", "Olivar Norte")
        coEvery { repository.findById("p-42") } returns plantation
        coEvery { updateHandler.handle(any()) } returns Result.failure(RuntimeException("DB error"))

        val vm = editViewModel("p-42")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.savedId)
    }

    private fun plantation(
        id: String = "test-id",
        name: String = "Test plantation",
        type: PlantationType = PlantationType.HUERTA,
        areaSqMeters: Double = 1000.0,
        plants: List<PlantType> = emptyList()
    ): Plantation = Plantation.reconstitute(
        id = id,
        name = name,
        type = type,
        location = Location(municipality = "Burgos", province = "Burgos"),
        areaSqMeters = areaSqMeters,
        plants = plants,
        notes = "",
        googleAccountEmail = null,
        createdAt = java.time.Instant.now(),
        updatedAt = java.time.Instant.now()
    )
}
