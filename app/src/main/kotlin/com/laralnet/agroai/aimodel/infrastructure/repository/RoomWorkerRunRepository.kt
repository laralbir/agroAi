package com.laralnet.agroai.aimodel.infrastructure.repository

import com.laralnet.agroai.aimodel.domain.model.WorkerRun
import com.laralnet.agroai.aimodel.domain.repository.WorkerRunRepository
import com.laralnet.agroai.aimodel.infrastructure.persistence.dao.WorkerRunDao
import com.laralnet.agroai.aimodel.infrastructure.persistence.entity.WorkerRunEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomWorkerRunRepository @Inject constructor(
    private val dao: WorkerRunDao
) : WorkerRunRepository {

    override fun observeAll(): Flow<List<WorkerRun>> =
        dao.observeAll().map { it.map(WorkerRunEntity::toDomain) }

    override fun observeByPlantation(plantationId: String): Flow<List<WorkerRun>> =
        dao.observeByPlantation(plantationId).map { it.map(WorkerRunEntity::toDomain) }

    override suspend fun findById(id: String): WorkerRun? =
        dao.findById(id)?.toDomain()

    override suspend fun save(run: WorkerRun) =
        dao.insert(run.toEntity())

    override suspend fun deleteOlderThan(cutoff: Instant) =
        dao.deleteOlderThan(cutoff.toEpochMilli())
}

private fun WorkerRunEntity.toDomain() = WorkerRun(
    id = id,
    timestamp = Instant.ofEpochMilli(timestamp),
    plantationId = plantationId,
    plantationName = plantationName,
    actionsCreated = actionsCreated,
    summary = summary,
    durationMs = durationMs
)

private fun WorkerRun.toEntity() = WorkerRunEntity(
    id = id,
    timestamp = timestamp.toEpochMilli(),
    plantationId = plantationId,
    plantationName = plantationName,
    actionsCreated = actionsCreated,
    summary = summary,
    durationMs = durationMs
)
