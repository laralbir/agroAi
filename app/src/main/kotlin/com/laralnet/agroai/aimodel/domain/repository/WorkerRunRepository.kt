package com.laralnet.agroai.aimodel.domain.repository

import com.laralnet.agroai.aimodel.domain.model.WorkerRun
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface WorkerRunRepository {
    fun observeAll(): Flow<List<WorkerRun>>
    fun observeByPlantation(plantationId: String): Flow<List<WorkerRun>>
    suspend fun findById(id: String): WorkerRun?
    suspend fun save(run: WorkerRun)
    suspend fun deleteOlderThan(cutoff: Instant)
}
