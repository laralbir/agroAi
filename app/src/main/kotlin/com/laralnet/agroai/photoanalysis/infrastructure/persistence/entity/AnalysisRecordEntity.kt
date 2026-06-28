package com.laralnet.agroai.photoanalysis.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_records")
data class AnalysisRecordEntity(
    @PrimaryKey val id: String,
    val plantationId: String?,
    val plantTypeId: String?,
    val plantationName: String?,
    val plantTypeName: String?,
    val rawResponse: String,
    val createdAtEpochMs: Long
)
