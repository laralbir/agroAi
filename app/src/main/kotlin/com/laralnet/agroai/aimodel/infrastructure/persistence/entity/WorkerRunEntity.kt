package com.laralnet.agroai.aimodel.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "worker_runs")
data class WorkerRunEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val plantationId: String?,
    val plantationName: String?,
    val actionsCreated: Int,
    val summary: String,
    val durationMs: Long
)
