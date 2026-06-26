package com.laralnet.agroai.treatment.application.command

import com.laralnet.agroai.treatment.domain.model.TreatmentType
import java.time.Instant

data class ScheduleTreatmentCommand(
    val plantationId: String,
    val type: TreatmentType,
    val title: String,
    val description: String = "",
    val scheduledAt: Instant,
    val calendarAccountEmail: String? = null,
    val addToCalendar: Boolean = false,
    val aiAnalysisResult: String? = null
)

data class CompleteTreatmentCommand(
    val treatmentId: String,
    val notes: String = "",
    val photoUri: String? = null,
    val aiAnalysisResult: String? = null
)

data class DeleteTreatmentCommand(val treatmentId: String)
