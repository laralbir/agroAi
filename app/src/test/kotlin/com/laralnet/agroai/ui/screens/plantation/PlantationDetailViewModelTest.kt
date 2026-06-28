package com.laralnet.agroai.ui.screens.plantation

import com.laralnet.agroai.plantation.application.command.UpdatePlantationCommand
import com.laralnet.agroai.plantation.application.handler.UpdatePlantationHandler
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import com.laralnet.agroai.ui.screens.plantation.detail.PlantationDetailViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import com.laralnet.agroai.weather.application.handler.RefreshWeatherHandler
import com.laralnet.agroai.weather.application.query.ObserveWeatherQuery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class PlantationDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val plantationRepository: PlantationRepository = mockk(relaxed = true)
    private val treatmentRepository: TreatmentRepository = mockk(relaxed = true)
    private val observeWeatherQuery: ObserveWeatherQuery = mockk(relaxed = true)
    private val refreshWeatherHandler: RefreshWeatherHandler = mockk(relaxed = true)
    private val updatePlantationHandler: UpdatePlantationHandler = mockk(relaxed = true)

    private fun viewModel() = PlantationDetailViewModel(
        plantationRepository = plantationRepository,
        treatmentRepository = treatmentRepository,
        observeWeatherQuery = observeWeatherQuery,
        refreshWeatherHandler = refreshWeatherHandler,
        updatePlantationHandler = updatePlantationHandler
    )

    private fun plantation(plants: List<PlantType> = emptyList()) = Plantation.reconstitute(
        id = "p1",
        name = "Test Plantation",
        type = PlantationType.HUERTA,
        location = Location(),
        areaSqMeters = 100.0,
        plants = plants,
        notes = "",
        googleAccountEmail = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun plantType(id: String = "pt1", name: String = "Tomato") = PlantType(
        id = id,
        plantationId = "p1",
        name = name,
        variety = "",
        count = 10
    )

    @Test
    fun `load sets plantation state`() = runTest {
        val p = plantation()
        coEvery { plantationRepository.findById("p1") } returns p
        coEvery { treatmentRepository.observeByPlantation("p1") } returns flowOf(emptyList())

        val vm = viewModel()
        vm.load("p1")
        advanceUntilIdle()

        assertEquals(p, vm.plantation.value)
    }

    @Test
    fun `load returns when plantation not found`() = runTest {
        coEvery { plantationRepository.findById("unknown") } returns null
        coEvery { treatmentRepository.observeByPlantation("unknown") } returns flowOf(emptyList())

        val vm = viewModel()
        vm.load("unknown")
        advanceUntilIdle()

        assertNull(vm.plantation.value)
    }

    @Test
    fun `deletePlantType removes plant from list and saves via UpdatePlantationHandler`() = runTest {
        val pt1 = plantType("pt1", "Tomato")
        val pt2 = plantType("pt2", "Pepper")
        val p = plantation(plants = listOf(pt1, pt2))

        coEvery { plantationRepository.findById("p1") } returns p
        coEvery { treatmentRepository.observeByPlantation("p1") } returns flowOf(emptyList())
        coEvery { updatePlantationHandler.handle(any()) } returns Result.success(Unit)

        val pAfterDelete = p.update(plants = listOf(pt2))
        coEvery { plantationRepository.findById("p1") } returnsMany listOf(p, pAfterDelete)

        val vm = viewModel()
        vm.load("p1")
        advanceUntilIdle()
        assertNotNull(vm.plantation.value)

        vm.deletePlantType("pt1")
        advanceUntilIdle()

        val commandSlot = slot<UpdatePlantationCommand>()
        coVerify { updatePlantationHandler.handle(capture(commandSlot)) }
        assertEquals(1, commandSlot.captured.plants.size)
        assertEquals("pt2", commandSlot.captured.plants[0].id)
    }

    @Test
    fun `deletePlantType does nothing when no plantation is loaded`() = runTest {
        val vm = viewModel()
        // No load() called — plantation is null

        vm.deletePlantType("pt1")
        advanceUntilIdle()

        coVerify(exactly = 0) { updatePlantationHandler.handle(any()) }
    }

    @Test
    fun `deletePlantType refreshes plantation state after deletion`() = runTest {
        val pt1 = plantType("pt1", "Tomato")
        val p = plantation(plants = listOf(pt1))

        coEvery { treatmentRepository.observeByPlantation("p1") } returns flowOf(emptyList())
        coEvery { updatePlantationHandler.handle(any()) } returns Result.success(Unit)

        val pAfterDelete = p.update(plants = emptyList())
        coEvery { plantationRepository.findById("p1") } returnsMany listOf(p, pAfterDelete)

        val vm = viewModel()
        vm.load("p1")
        advanceUntilIdle()

        vm.deletePlantType("pt1")
        advanceUntilIdle()

        assertEquals(emptyList<PlantType>(), vm.plantation.value?.plants)
    }

    @Test
    fun `updatePlantType replaces the matching plant in the list`() = runTest {
        val original = plantType("pt1", "Tomato")
        val p = plantation(plants = listOf(original))

        coEvery { treatmentRepository.observeByPlantation("p1") } returns flowOf(emptyList())
        coEvery { updatePlantationHandler.handle(any()) } returns Result.success(Unit)

        val updated = original.copy(name = "Cherry Tomato", count = 20)
        val pAfterUpdate = p.update(plants = listOf(updated))
        coEvery { plantationRepository.findById("p1") } returnsMany listOf(p, pAfterUpdate)

        val vm = viewModel()
        vm.load("p1")
        advanceUntilIdle()

        vm.updatePlantType(updated)
        advanceUntilIdle()

        val commandSlot = slot<UpdatePlantationCommand>()
        coVerify { updatePlantationHandler.handle(capture(commandSlot)) }
        assertEquals("Cherry Tomato", commandSlot.captured.plants[0].name)
        assertEquals(20, commandSlot.captured.plants[0].count)
    }
}
