package com.laralnet.agroai.plantation.application

import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.plantation.application.command.CreatePlantationCommand
import com.laralnet.agroai.plantation.application.handler.CreatePlantationHandler
import com.laralnet.agroai.plantation.domain.event.PlantationCreated
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreatePlantationHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository: PlantationRepository = mockk(relaxed = true)
    private val eventBus: EventBus = mockk(relaxed = true)
    private val handler = CreatePlantationHandler(repository, eventBus)

    private val command = CreatePlantationCommand(
        name = "Viñedo Ribera",
        type = PlantationType.VIÑEDO,
        location = Location(municipality = "Roa", province = "Burgos"),
        areaSqMeters = 8000.0,
        notes = "Tempranillo"
    )

    @Test
    fun `handle() saves plantation to repository`() = runTest {
        val result = handler.handle(command)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `handle() returns plantation with correct name`() = runTest {
        val result = handler.handle(command)

        assertEquals("Viñedo Ribera", result.getOrThrow().name)
    }

    @Test
    fun `handle() publishes PlantationCreated event to EventBus`() = runTest {
        handler.handle(command)

        coVerify { eventBus.publish(match { it is PlantationCreated }) }
    }

    @Test
    fun `handle() returns failure when repository throws`() = runTest {
        coEvery { repository.save(any()) } throws RuntimeException("DB error")

        val result = handler.handle(command)

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `handle() saved plantation has a non-blank id`() = runTest {
        val result = handler.handle(command)

        assertTrue(result.getOrThrow().id.isNotBlank())
    }
}
