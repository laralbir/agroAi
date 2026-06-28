package com.laralnet.agroai.photoanalysis.infrastructure.persistence.mapper

import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import com.laralnet.agroai.photoanalysis.infrastructure.persistence.entity.AnalysisRecordEntity
import java.time.Instant

fun AnalysisRecord.toEntity() = AnalysisRecordEntity(
    id = id,
    plantationId = plantationId,
    plantTypeId = plantTypeId,
    plantationName = plantationName,
    plantTypeName = plantTypeName,
    rawResponse = rawResponse,
    createdAtEpochMs = createdAt.toEpochMilli()
)

fun AnalysisRecordEntity.toDomain() = AnalysisRecord(
    id = id,
    plantationId = plantationId,
    plantTypeId = plantTypeId,
    plantationName = plantationName,
    plantTypeName = plantTypeName,
    rawResponse = rawResponse,
    createdAt = Instant.ofEpochMilli(createdAtEpochMs)
)
