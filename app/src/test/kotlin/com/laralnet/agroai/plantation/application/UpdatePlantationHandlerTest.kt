package com.laralnet.agroai.plantation.application

import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.plantation.application.command.UpdatePlantationCommand
import com.laralnet.agroai.plantation.application.handler.UpdatePlantationHandler
import com.laralnet.agroai.plantation.domain.event.PlantationUpdated
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdatePlantationHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: PlantationRepository = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = UpdatePlantationHandler(repository, eventBus)

    private val existing = plantation(name = "Viñedo original", type = PlantationType.VIÑEDO)

    @Test
    fun `handle() saves updated plantation to repository`() = runTest {
        coEvery { repository.findById(existing.id) } returns existing

        val result = handler.handle(command(id = existing.id, name = "Viñedo renovado"))

        assertTrue(result.isSuccess)
        coVerify { repository.save(match { it.id == existing.id && it.name == "Viñedo renovado" }) }
    }

    @Test
    fun `handle() publishes PlantationUpdated event`() = runTest {
        coEvery { repository.findById(existing.id) } returns existing

        handler.handle(command(id = existing.id))

        coVerify { eventBus.publish(match { it is PlantationUpdated && (it as PlantationUpdated).plantationId == existing.id }) }
    }

    @Test
    fun `handle() sets correct plantationId on all plants`() = runTest {
        coEvery { repository.findById(existing.id) } returns existing
        val plant = PlantType(plantationId = "", name = "Tempranillo", variety = "Clásica", count = 200)

        handler.handle(command(id = existing.id, plants = listOf(plant)))

        coVerify { repository.save(match { saved -> saved.plants.all { it.plantationId == existing.id } }) }
    }

    @Test
    fun `handle() returns failure when plantation not found`() = runTest {
        coEvery { repository.findById(any()) } returns null

        val result = handler.handle(command(id = "missing-id"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `handle() preserves plantation id after update`() = runTest {
        coEvery { repository.findById(existing.id) } returns existing

        handler.handle(command(id = existing.id, name = "Nuevo nombre"))

        coVerify { repository.save(match { it.id == existing.id }) }
    }

    @Test
    fun `handle() updates area correctly`() = runTest {
        coEvery { repository.findById(existing.id) } returns existing

        handler.handle(command(id = existing.id, areaSqMeters = 15000.0))

        coVerify { repository.save(match { it.areaSqMeters == 15000.0 }) }
    }

    private fun plantation(
        name: String = "Test plantation",
        type: PlantationType = PlantationType.HUERTA
    ): Plantation = Plantation.create(
        name = name,
        type = type,
        location = Location(municipality = "Burgos", province = "Burgos"),
        areaSqMeters = 5000.0
    )

    private fun command(
        id: String = existing.id,
        name: String = "Updated name",
        type: PlantationType = PlantationType.VIÑEDO,
        areaSqMeters: Double = 8000.0,
        plants: List<PlantType> = emptyList()
    ) = UpdatePlantationCommand(
        id = id,
        name = name,
        type = type,
        location = Location(municipality = "Roa", province = "Burgos"),
        areaSqMeters = areaSqMeters,
        plants = plants,
        notes = "",
        googleAccountEmail = null
    )
}
