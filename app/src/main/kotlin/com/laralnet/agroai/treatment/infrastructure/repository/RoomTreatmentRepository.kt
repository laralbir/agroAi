package com.laralnet.agroai.treatment.infrastructure.repository

import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentRecord
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import com.laralnet.agroai.treatment.infrastructure.persistence.dao.TreatmentDao
import com.laralnet.agroai.treatment.infrastructure.persistence.entity.TreatmentEntity
import com.laralnet.agroai.treatment.infrastructure.persistence.entity.TreatmentRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomTreatmentRepository @Inject constructor(
    private val dao: TreatmentDao
) : TreatmentRepository {

    override fun observeByPlantation(plantationId: String): Flow<List<Treatment>> =
        dao.observeByPlantation(plantationId).map { list -> list.map { it.toDomain() } }

    override fun observeRecordsByPlantation(plantationId: String): Flow<List<TreatmentRecord>> =
        dao.observeRecordsByPlantation(plantationId).map { list -> list.map { it.toDomain() } }

    override suspend fun findById(id: String): Treatment? = dao.findById(id)?.toDomain()

    override suspend fun save(treatment: Treatment) = dao.insert(treatment.toEntity())

    override suspend fun saveRecord(record: TreatmentRecord) = dao.insertRecord(record.toEntity())

    override suspend fun delete(id: String) = dao.delete(id)

    override fun observeUpcoming(): Flow<List<Treatment>> =
        dao.observeUpcoming().map { list -> list.map { it.toDomain() } }

    override fun observeByDateRange(start: Instant, end: Instant): Flow<List<Treatment>> =
        dao.observeByDateRange(start.toEpochMilli(), end.toEpochMilli()).map { list -> list.map { it.toDomain() } }

    private fun TreatmentEntity.toDomain() = Treatment(
        id = id, plantationId = plantationId,
        type = type, title = title, description = description,
        scheduledAt = Instant.ofEpochMilli(scheduledAt),
        status = status, calendarEventId = calendarEventId,
        calendarAccountEmail = calendarAccountEmail,
        aiAnalysisResult = aiAnalysisResult,
        createdAt = Instant.ofEpochMilli(createdAt)
    )

    private fun TreatmentRecordEntity.toDomain() = TreatmentRecord(
        id = id, treatmentId = treatmentId, plantationId = plantationId,
        executedAt = Instant.ofEpochMilli(executedAt),
        notes = notes, photoUri = photoUri, aiAnalysisResult = aiAnalysisResult
    )

    private fun Treatment.toEntity() = TreatmentEntity(
        id = id, plantationId = plantationId,
        type = type, title = title, description = description,
        scheduledAt = scheduledAt.toEpochMilli(),
        status = status, calendarEventId = calendarEventId,
        calendarAccountEmail = calendarAccountEmail,
        aiAnalysisResult = aiAnalysisResult,
        createdAt = createdAt.toEpochMilli()
    )

    private fun TreatmentRecord.toEntity() = TreatmentRecordEntity(
        id = id, treatmentId = treatmentId, plantationId = plantationId,
        executedAt = executedAt.toEpochMilli(),
        notes = notes, photoUri = photoUri, aiAnalysisResult = aiAnalysisResult
    )
}
