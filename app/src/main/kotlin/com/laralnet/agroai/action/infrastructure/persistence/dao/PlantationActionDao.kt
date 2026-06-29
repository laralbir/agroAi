package com.laralnet.agroai.action.infrastructure.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laralnet.agroai.action.infrastructure.persistence.entity.PlantationActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantationActionDao {

    @Query("SELECT * FROM plantation_actions WHERE plantationId = :plantationId ORDER BY scheduledAt ASC")
    fun observeByPlantation(plantationId: String): Flow<List<PlantationActionEntity>>

    @Query("SELECT * FROM plantation_actions WHERE status = 'PENDING' ORDER BY scheduledAt ASC")
    fun observeUpcoming(): Flow<List<PlantationActionEntity>>

    @Query("SELECT * FROM plantation_actions WHERE plantationId = :plantationId AND source = 'AI' AND status = 'PENDING'")
    fun observePendingAiByPlantation(plantationId: String): Flow<List<PlantationActionEntity>>

    @Query("SELECT * FROM plantation_actions WHERE id = :id")
    suspend fun findById(id: String): PlantationActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlantationActionEntity)

    @Query("DELETE FROM plantation_actions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM plantation_actions WHERE plantationId = :plantationId AND source = 'AI' AND status = 'PENDING'")
    suspend fun deleteAiPendingByPlantation(plantationId: String)
}
