package com.laralnet.agroai.action.domain.repository

import com.laralnet.agroai.action.domain.model.PlantationAction
import kotlinx.coroutines.flow.Flow

interface PlantationActionRepository {
    fun observeByPlantation(plantationId: String): Flow<List<PlantationAction>>
    fun observeUpcoming(): Flow<List<PlantationAction>>
    fun observePendingAiByPlantation(plantationId: String): Flow<List<PlantationAction>>
    suspend fun findById(id: String): PlantationAction?
    suspend fun save(action: PlantationAction)
    suspend fun delete(id: String)
    suspend fun deleteAiPendingByPlantation(plantationId: String)
}
