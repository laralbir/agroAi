package com.laralnet.agroai.plantation.infrastructure.repository

import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.plantation.infrastructure.persistence.dao.PlantationDao
import com.laralnet.agroai.plantation.infrastructure.persistence.mapper.toDomain
import com.laralnet.agroai.plantation.infrastructure.persistence.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomPlantationRepository @Inject constructor(
    private val dao: PlantationDao
) : PlantationRepository {

    override fun observeAll(): Flow<List<Plantation>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun findById(id: String): Plantation? =
        dao.findById(id)?.toDomain()

    override suspend fun save(plantation: Plantation) {
        dao.upsert(
            plantation = plantation.toEntity(),
            plants = plantation.plants.map { it.toEntity() }
        )
    }

    override suspend fun delete(id: String) = dao.delete(id)
}
