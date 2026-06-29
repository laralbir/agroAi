package com.laralnet.agroai.aimodel.infrastructure.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laralnet.agroai.aimodel.infrastructure.persistence.entity.WorkerRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerRunDao {

    @Query("SELECT * FROM worker_runs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WorkerRunEntity>>

    @Query("SELECT * FROM worker_runs WHERE plantationId = :plantationId ORDER BY timestamp DESC")
    fun observeByPlantation(plantationId: String): Flow<List<WorkerRunEntity>>

    @Query("SELECT * FROM worker_runs WHERE id = :id")
    suspend fun findById(id: String): WorkerRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkerRunEntity)

    @Query("DELETE FROM worker_runs WHERE timestamp < :cutoffEpochMs")
    suspend fun deleteOlderThan(cutoffEpochMs: Long)
}
