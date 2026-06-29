package com.laralnet.agroai.action.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plantation_actions")
data class PlantationActionEntity(
    @PrimaryKey val id: String,
    val plantationId: String,
    val plantTypeId: String?,
    val actionType: String,
    val title: String,
    val notes: String,
    val scheduledAt: Long,
    val status: String,
    val calendarEventId: Long?,
    val calendarAccountEmail: String?,
    val source: String,
    val createdAt: Long
)
