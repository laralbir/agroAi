package com.laralnet.agroai.plantation.domain.event

import com.laralnet.agroai.core.domain.event.DomainEvent

class PlantationCreated(
    val plantationId: String,
    val name: String
) : DomainEvent()

class PlantationUpdated(
    val plantationId: String
) : DomainEvent()

class PlantationDeleted(
    val plantationId: String
) : DomainEvent()
