package com.laralnet.agroai.treatment.domain.event

import com.laralnet.agroai.core.domain.event.DomainEvent

class TreatmentScheduled(val treatmentId: String, val plantationId: String) : DomainEvent()
class TreatmentCompleted(val treatmentId: String, val plantationId: String) : DomainEvent()
class TreatmentDeleted(val treatmentId: String, val plantationId: String) : DomainEvent()
