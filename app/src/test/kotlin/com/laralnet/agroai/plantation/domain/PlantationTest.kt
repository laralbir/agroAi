package com.laralnet.agroai.plantation.domain

import com.laralnet.agroai.plantation.domain.event.PlantationCreated
import com.laralnet.agroai.plantation.domain.event.PlantationUpdated
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantationTest {

    private val defaultLocation = Location(municipality = "Valencia", province = "Valencia")

    @Test
    fun `create() produces a PlantationCreated domain event`() {
        val plantation = Plantation.create(
            name = "Mi huerta",
            type = PlantationType.HUERTA,
            location = defaultLocation,
            areaSqMeters = 250.0
        )

        assertEquals(1, plantation.domainEvents.size)
        val event = plantation.domainEvents.first()
        assertNotNull(event as? PlantationCreated)
        assertEquals("Mi huerta", (event as PlantationCreated).name)
        assertEquals(plantation.id, event.plantationId)
    }

    @Test
    fun `create() stores correct field values`() {
        val plantation = Plantation.create(
            name = "Olivar del Norte",
            type = PlantationType.OLIVAR,
            location = defaultLocation,
            areaSqMeters = 5000.0,
            notes = "Zona seca"
        )

        assertEquals("Olivar del Norte", plantation.name)
        assertEquals(PlantationType.OLIVAR, plantation.type)
        assertEquals(5000.0, plantation.areaSqMeters, 0.01)
        assertEquals("Zona seca", plantation.notes)
    }

    @Test
    fun `update() produces PlantationUpdated event and keeps original id`() {
        val original = Plantation.create(
            name = "Original",
            type = PlantationType.CEREAL,
            location = defaultLocation,
            areaSqMeters = 100.0
        )
        original.clearDomainEvents()

        val updated = original.update(name = "Actualizada", areaSqMeters = 200.0)

        assertEquals(original.id, updated.id)
        assertEquals("Actualizada", updated.name)
        assertEquals(200.0, updated.areaSqMeters, 0.01)
        assertEquals(1, updated.domainEvents.size)
        assertTrue(updated.domainEvents.first() is PlantationUpdated)
    }

    @Test
    fun `clearDomainEvents() empties the events list`() {
        val plantation = Plantation.create(
            name = "Test",
            type = PlantationType.HUERTA,
            location = defaultLocation,
            areaSqMeters = 100.0
        )

        assertTrue(plantation.domainEvents.isNotEmpty())
        plantation.clearDomainEvents()
        assertTrue(plantation.domainEvents.isEmpty())
    }

    @Test
    fun `create() without google account leaves it null`() {
        val plantation = Plantation.create(
            name = "Sin cuenta",
            type = PlantationType.REGADIO,
            location = defaultLocation,
            areaSqMeters = 10.0
        )

        assertTrue(plantation.googleAccountEmail == null)
    }

    @Test
    fun `location displayAddress is built correctly from parts`() {
        val location = Location(
            address = "Camino Real 12",
            municipality = "Murcia",
            province = "Murcia"
        )

        assertTrue(location.displayAddress.contains("Murcia"))
    }
}
