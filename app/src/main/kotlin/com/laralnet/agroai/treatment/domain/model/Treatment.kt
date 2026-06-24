package com.laralnet.agroai.treatment.domain.model

import java.time.Instant
import java.util.UUID

enum class TreatmentType {
    RIEGO, PODA, COSECHA, FERTILIZACION, FUMIGACION, INJERTO, TRANSPLANTE, OTRO
}

enum class TreatmentStatus { PENDING, DONE, SKIPPED, RESCHEDULED }

data class Treatment(
    val id: String = UUID.randomUUID().toString(),
    val plantationId: String,
    val type: TreatmentType,
    val title: String,
    val description: String = "",
    val scheduledAt: Instant,
    val status: TreatmentStatus = TreatmentStatus.PENDING,
    val calendarEventId: Long? = null,
    val calendarAccountEmail: String? = null,
    val createdAt: Instant = Instant.now()
)

data class TreatmentRecord(
    val id: String = UUID.randomUUID().toString(),
    val treatmentId: String,
    val plantationId: String,
    val executedAt: Instant,
    val notes: String = "",
    val photoUri: String? = null,
    val aiAnalysisResult: String? = null
)
