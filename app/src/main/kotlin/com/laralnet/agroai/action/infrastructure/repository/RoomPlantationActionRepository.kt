package com.laralnet.agroai.action.infrastructure.repository

import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.action.infrastructure.persistence.dao.PlantationActionDao
import com.laralnet.agroai.action.infrastructure.persistence.mapper.toDomain
import com.laralnet.agroai.action.infrastructure.persistence.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomPlantationActionRepository @Inject constructor(
    private val dao: PlantationActionDao
) : PlantationActionRepository {

    override fun observeByPlantation(plantationId: String): Flow<List<PlantationAction>> =
        dao.observeByPlantation(plantationId).map { list -> list.map { it.toDomain() } }

    override fun observeUpcoming(): Flow<List<PlantationAction>> =
        dao.observeUpcoming().map { list -> list.map { it.toDomain() } }

    override fun observePendingAiByPlantation(plantationId: String): Flow<List<PlantationAction>> =
        dao.observePendingAiByPlantation(plantationId).map { list -> list.map { it.toDomain() } }

    override suspend fun findById(id: String): PlantationAction? =
        dao.findById(id)?.toDomain()

    override suspend fun save(action: PlantationAction) =
        dao.insert(action.toEntity())

    override suspend fun delete(id: String) =
        dao.delete(id)

    override suspend fun deleteAiPendingByPlantation(plantationId: String) =
        dao.deleteAiPendingByPlantation(plantationId)
}
