package com.laralnet.agroai.aimodel.domain.model

import java.time.Instant

data class WorkerRun(
    val id: String,
    val timestamp: Instant,
    val plantationId: String?,
    val plantationName: String?,
    val actionsCreated: Int,
    val summary: String,
    val durationMs: Long
)
