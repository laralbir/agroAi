package com.laralnet.agroai.photoanalysis.infrastructure.repository

import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import com.laralnet.agroai.photoanalysis.domain.repository.AnalysisRepository
import com.laralnet.agroai.photoanalysis.infrastructure.persistence.dao.AnalysisRecordDao
import com.laralnet.agroai.photoanalysis.infrastructure.persistence.mapper.toDomain
import com.laralnet.agroai.photoanalysis.infrastructure.persistence.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomAnalysisRepository @Inject constructor(
    private val dao: AnalysisRecordDao
) : AnalysisRepository {

    override suspend fun save(record: AnalysisRecord) = dao.insert(record.toEntity())

    override suspend fun findById(id: String): AnalysisRecord? = dao.findById(id)?.toDomain()

    override fun observeAll(): Flow<List<AnalysisRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByPlantation(plantationId: String): Flow<List<AnalysisRecord>> =
        dao.observeByPlantation(plantationId).map { list -> list.map { it.toDomain() } }

    override suspend fun delete(id: String) = dao.deleteById(id)
}
