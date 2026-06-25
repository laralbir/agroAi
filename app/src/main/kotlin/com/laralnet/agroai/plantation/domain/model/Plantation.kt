package com.laralnet.agroai.plantation.domain.model

import com.laralnet.agroai.plantation.domain.event.PlantationCreated
import com.laralnet.agroai.plantation.domain.event.PlantationUpdated
import com.laralnet.agroai.core.domain.event.DomainEvent
import java.time.Instant
import java.util.UUID

class Plantation private constructor(
    val id: String,
    val name: String,
    val type: PlantationType,
    val location: Location,
    val areaSqMeters: Double,
    val plants: List<PlantType>,
    val notes: String,
    val googleAccountEmail: String?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    private val _domainEvents = mutableListOf<DomainEvent>()
    val domainEvents: List<DomainEvent> get() = _domainEvents.toList()

    fun clearDomainEvents() = _domainEvents.clear()

    companion object {
        fun create(
            name: String,
            type: PlantationType,
            location: Location,
            areaSqMeters: Double,
            plants: List<PlantType> = emptyList(),
            notes: String = "",
            googleAccountEmail: String? = null
        ): Plantation {
            val id = UUID.randomUUID().toString()
            val now = Instant.now()
            return Plantation(
                id = id,
                name = name,
                type = type,
                location = location,
                areaSqMeters = areaSqMeters,
                plants = plants.map { it.copy(plantationId = id) },
                notes = notes,
                googleAccountEmail = googleAccountEmail,
                createdAt = now,
                updatedAt = now
            ).also { plantation ->
                plantation._domainEvents.add(PlantationCreated(plantationId = id, name = name))
            }
        }

        fun reconstitute(
            id: String,
            name: String,
            type: PlantationType,
            location: Location,
            areaSqMeters: Double,
            plants: List<PlantType>,
            notes: String,
            googleAccountEmail: String?,
            createdAt: Instant,
            updatedAt: Instant
        ): Plantation = Plantation(
            id, name, type, location, areaSqMeters, plants, notes,
            googleAccountEmail, createdAt, updatedAt
        )
    }

    fun update(
        name: String = this.name,
        type: PlantationType = this.type,
        location: Location = this.location,
        areaSqMeters: Double = this.areaSqMeters,
        plants: List<PlantType> = this.plants,
        notes: String = this.notes,
        googleAccountEmail: String? = this.googleAccountEmail
    ): Plantation {
        val updated = Plantation(
            id = id,
            name = name,
            type = type,
            location = location,
            areaSqMeters = areaSqMeters,
            plants = plants.map { it.copy(plantationId = id) },
            notes = notes,
            googleAccountEmail = googleAccountEmail,
            createdAt = createdAt,
            updatedAt = Instant.now()
        )
        updated._domainEvents.add(PlantationUpdated(plantationId = id))
        return updated
    }
}
