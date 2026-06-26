package com.laralnet.agroai.treatment.infrastructure.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laralnet.agroai.treatment.infrastructure.persistence.entity.TreatmentEntity
import com.laralnet.agroai.treatment.infrastructure.persistence.entity.TreatmentRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreatmentDao {

    @Query("SELECT * FROM treatments WHERE plantationId = :plantationId ORDER BY scheduledAt ASC")
    fun observeByPlantation(plantationId: String): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatment_records WHERE plantationId = :plantationId ORDER BY executedAt DESC")
    fun observeRecordsByPlantation(plantationId: String): Flow<List<TreatmentRecordEntity>>

    @Query("SELECT * FROM treatments WHERE id = :id")
    suspend fun findById(id: String): TreatmentEntity?

    @Query("SELECT * FROM treatments WHERE status = 'PENDING' ORDER BY scheduledAt ASC")
    fun observeUpcoming(): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatments WHERE scheduledAt >= :startMs AND scheduledAt < :endMs ORDER BY scheduledAt ASC")
    fun observeByDateRange(startMs: Long, endMs: Long): Flow<List<TreatmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: TreatmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TreatmentRecordEntity)

    @Query("DELETE FROM treatments WHERE id = :id")
    suspend fun delete(id: String)
}
