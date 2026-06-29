package com.laralnet.agroai.action.infrastructure.persistence.mapper

import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.infrastructure.persistence.entity.PlantationActionEntity
import java.time.Instant

fun PlantationActionEntity.toDomain() = PlantationAction(
    id = id,
    plantationId = plantationId,
    plantTypeId = plantTypeId,
    actionType = runCatching { ActionType.valueOf(actionType) }.getOrDefault(ActionType.OTRO),
    title = title,
    notes = notes,
    scheduledAt = Instant.ofEpochMilli(scheduledAt),
    status = runCatching { ActionStatus.valueOf(status) }.getOrDefault(ActionStatus.PENDING),
    calendarEventId = calendarEventId,
    calendarAccountEmail = calendarAccountEmail,
    source = runCatching { ActionSource.valueOf(source) }.getOrDefault(ActionSource.MANUAL),
    createdAt = Instant.ofEpochMilli(createdAt)
)

fun PlantationAction.toEntity() = PlantationActionEntity(
    id = id,
    plantationId = plantationId,
    plantTypeId = plantTypeId,
    actionType = actionType.name,
    title = title,
    notes = notes,
    scheduledAt = scheduledAt.toEpochMilli(),
    status = status.name,
    calendarEventId = calendarEventId,
    calendarAccountEmail = calendarAccountEmail,
    source = source.name,
    createdAt = createdAt.toEpochMilli()
)
