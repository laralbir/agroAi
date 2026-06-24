package com.laralnet.agroai.plantation.infrastructure.persistence.mapper

import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationEntity
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationWithPlants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PlantationMapperTest {

    private val now = Instant.now()

    private val entity = PlantationEntity(
        id = "abc-123",
        name = "Huerta de prueba",
        type = PlantationType.HUERTA,
        latitude = 40.4168,
        longitude = -3.7038,
        address = "Calle Mayor 1",
        municipality = "Madrid",
        province = "Madrid",
        country = "ES",
        municipalityCode = "28079",
        areaSqMeters = 500.0,
        notes = "Riego por goteo",
        googleAccountEmail = "user@gmail.com",
        createdAt = now.toEpochMilli(),
        updatedAt = now.toEpochMilli()
    )

    @Test
    fun `toDomain() maps id correctly`() {
        val domain = PlantationWithPlants(entity, emptyList()).toDomain()
        assertEquals("abc-123", domain.id)
    }

    @Test
    fun `toDomain() maps location coordinates`() {
        val domain = PlantationWithPlants(entity, emptyList()).toDomain()
        assertEquals(40.4168, domain.location.latitude!!, 0.0001)
        assertEquals(-3.7038, domain.location.longitude!!, 0.0001)
    }

    @Test
    fun `toDomain() maps municipality code`() {
        val domain = PlantationWithPlants(entity, emptyList()).toDomain()
        assertEquals("28079", domain.location.municipalityCode)
    }

    @Test
    fun `toEntity() round-trips name and type`() {
        val plantation = Plantation.create(
            name = "Olivar Test",
            type = PlantationType.OLIVAR,
            location = Location(municipality = "Jaén", province = "Jaén"),
            areaSqMeters = 3000.0
        )

        val result = plantation.toEntity()

        assertEquals("Olivar Test", result.name)
        assertEquals(PlantationType.OLIVAR, result.type)
        assertEquals(3000.0, result.areaSqMeters, 0.01)
    }

    @Test
    fun `toDomain() with null coordinates leaves location without coordinates`() {
        val noCoordEntity = entity.copy(latitude = null, longitude = null)
        val domain = PlantationWithPlants(noCoordEntity, emptyList()).toDomain()
        assertNull(domain.location.latitude)
        assertNull(domain.location.longitude)
    }
}
