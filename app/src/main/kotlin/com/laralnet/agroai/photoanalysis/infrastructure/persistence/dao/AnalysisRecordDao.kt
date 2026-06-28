package com.laralnet.agroai.photoanalysis.infrastructure.persistence.dao

import androidx.room.*
import com.laralnet.agroai.photoanalysis.infrastructure.persistence.entity.AnalysisRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisRecordDao {

    @Query("SELECT * FROM analysis_records ORDER BY createdAtEpochMs DESC LIMIT 100")
    fun observeAll(): Flow<List<AnalysisRecordEntity>>

    @Query("SELECT * FROM analysis_records WHERE plantationId = :plantationId ORDER BY createdAtEpochMs DESC")
    fun observeByPlantation(plantationId: String): Flow<List<AnalysisRecordEntity>>

    @Query("SELECT * FROM analysis_records WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AnalysisRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnalysisRecordEntity)

    @Query("DELETE FROM analysis_records WHERE id = :id")
    suspend fun deleteById(id: String)
}
