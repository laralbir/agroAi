package com.laralnet.agroai.action.domain.event

import com.laralnet.agroai.core.domain.event.DomainEvent

class PlantationActionScheduled(val actionId: String, val plantationId: String) : DomainEvent()
class PlantationActionCompleted(val actionId: String, val plantationId: String) : DomainEvent()
class PlantationActionDeleted(val actionId: String, val plantationId: String) : DomainEvent()
