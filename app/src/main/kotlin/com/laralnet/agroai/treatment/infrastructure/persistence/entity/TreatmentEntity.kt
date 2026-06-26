package com.laralnet.agroai.treatment.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.treatment.domain.model.TreatmentType

@Entity(tableName = "treatments")
data class TreatmentEntity(
    @PrimaryKey val id: String,
    val plantationId: String,
    val type: TreatmentType,
    val title: String,
    val description: String,
    val scheduledAt: Long,
    val status: TreatmentStatus,
    val calendarEventId: Long?,
    val calendarAccountEmail: String?,
    val aiAnalysisResult: String?,
    val createdAt: Long
)

@Entity(tableName = "treatment_records")
data class TreatmentRecordEntity(
    @PrimaryKey val id: String,
    val treatmentId: String,
    val plantationId: String,
    val executedAt: Long,
    val notes: String,
    val photoUri: String?,
    val aiAnalysisResult: String?
)
