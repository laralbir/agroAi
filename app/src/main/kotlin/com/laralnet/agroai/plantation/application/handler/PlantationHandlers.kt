package com.laralnet.agroai.plantation.application.handler

import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.plantation.application.command.CreatePlantationCommand
import com.laralnet.agroai.plantation.application.command.DeletePlantationCommand
import com.laralnet.agroai.plantation.application.command.UpdatePlantationCommand
import com.laralnet.agroai.plantation.domain.event.PlantationDeleted
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import javax.inject.Inject

class CreatePlantationHandler @Inject constructor(
    private val repository: PlantationRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: CreatePlantationCommand): Result<Plantation> = runCatching {
        val plantation = Plantation.create(
            name = command.name,
            type = command.type,
            location = command.location,
            areaSqMeters = command.areaSqMeters,
            plants = command.plants,
            notes = command.notes,
            googleAccountEmail = command.googleAccountEmail
        )
        repository.save(plantation)
        plantation.domainEvents.forEach { eventBus.publish(it) }
        plantation.clearDomainEvents()
        plantation
    }
}

class UpdatePlantationHandler @Inject constructor(
    private val repository: PlantationRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: UpdatePlantationCommand): Result<Unit> = runCatching {
        val existing = repository.findById(command.id)
            ?: error("Plantation ${command.id} not found")
        val updated = existing.update(
            name = command.name,
            type = command.type,
            location = command.location,
            areaSqMeters = command.areaSqMeters,
            plants = command.plants,
            notes = command.notes,
            googleAccountEmail = command.googleAccountEmail
        )
        repository.save(updated)
        updated.domainEvents.forEach { eventBus.publish(it) }
        updated.clearDomainEvents()
    }
}

class DeletePlantationHandler @Inject constructor(
    private val repository: PlantationRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: DeletePlantationCommand): Result<Unit> = runCatching {
        repository.delete(command.id)
        eventBus.publish(PlantationDeleted(plantationId = command.id))
    }
}
