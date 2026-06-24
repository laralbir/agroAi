package com.laralnet.agroai.ui.wizard

import com.laralnet.agroai.plantation.application.handler.CreatePlantationHandler
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val handler: CreatePlantationHandler = mockk()
    private val viewModel = PlantationWizardViewModel(handler)

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
    fun `createPlantation sets createdId on success`() = runTest {
        val fakePlantation = Plantation.create(
            name = "Test",
            type = PlantationType.HUERTA,
            location = Location(),
            areaSqMeters = 100.0
        )
        coEvery { handler.handle(any()) } returns Result.success(fakePlantation)

        viewModel.setName("Test")
        viewModel.createPlantation()

        assertEquals(fakePlantation.id, viewModel.uiState.value.createdId)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `createPlantation sets error on failure`() = runTest {
        coEvery { handler.handle(any()) } returns Result.failure(RuntimeException("DB error"))

        viewModel.createPlantation()

        assertNotNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.createdId)
    }
}
