package com.laralnet.agroai.photoanalysis.domain.model

import java.time.Instant
import java.util.UUID

data class AnalysisRecord(
    val id: String = UUID.randomUUID().toString(),
    val plantationId: String?,
    val plantTypeId: String?,
    val plantationName: String?,
    val plantTypeName: String?,
    val rawResponse: String,
    val createdAt: Instant = Instant.now()
)
