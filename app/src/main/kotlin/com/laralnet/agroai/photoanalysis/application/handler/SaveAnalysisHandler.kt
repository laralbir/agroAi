package com.laralnet.agroai.photoanalysis.application.handler

import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import com.laralnet.agroai.photoanalysis.domain.repository.AnalysisRepository
import javax.inject.Inject

data class SaveAnalysisCommand(
    val plantationId: String?,
    val plantTypeId: String?,
    val plantationName: String?,
    val plantTypeName: String?,
    val rawResponse: String
)

class SaveAnalysisHandler @Inject constructor(
    private val repository: AnalysisRepository
) {
    suspend fun handle(command: SaveAnalysisCommand): Result<AnalysisRecord> = runCatching {
        val record = AnalysisRecord(
            plantationId = command.plantationId,
            plantTypeId = command.plantTypeId,
            plantationName = command.plantationName,
            plantTypeName = command.plantTypeName,
            rawResponse = command.rawResponse
        )
        repository.save(record)
        record
    }
}
