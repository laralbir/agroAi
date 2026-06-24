package com.laralnet.agroai.plantation.domain.repository

import com.laralnet.agroai.plantation.domain.model.Plantation
import kotlinx.coroutines.flow.Flow

interface PlantationRepository {
    fun observeAll(): Flow<List<Plantation>>
    suspend fun findById(id: String): Plantation?
    suspend fun save(plantation: Plantation)
    suspend fun delete(id: String)
}
