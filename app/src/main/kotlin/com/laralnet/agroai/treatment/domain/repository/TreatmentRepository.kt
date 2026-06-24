package com.laralnet.agroai.treatment.domain.repository

import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentRecord
import kotlinx.coroutines.flow.Flow

interface TreatmentRepository {
    fun observeByPlantation(plantationId: String): Flow<List<Treatment>>
    fun observeRecordsByPlantation(plantationId: String): Flow<List<TreatmentRecord>>
    suspend fun findById(id: String): Treatment?
    suspend fun save(treatment: Treatment)
    suspend fun saveRecord(record: TreatmentRecord)
    suspend fun delete(id: String)
    fun observeUpcoming(): Flow<List<Treatment>>
}
