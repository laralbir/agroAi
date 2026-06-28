package com.laralnet.agroai.photoanalysis.domain.repository

import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    suspend fun save(record: AnalysisRecord)
    suspend fun findById(id: String): AnalysisRecord?
    fun observeAll(): Flow<List<AnalysisRecord>>
    fun observeByPlantation(plantationId: String): Flow<List<AnalysisRecord>>
    suspend fun delete(id: String)
}
