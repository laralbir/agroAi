package com.laralnet.agroai.action.domain.model

import java.time.Instant
import java.util.UUID

enum class ActionType {
    REGAR, PODAR, CAVAR, FERTILIZAR, FUMIGAR, COSECHAR, INJERTAR, TRASPLANTAR, ABONAR, AIREAR, ACLARAR, OTRO
}

enum class ActionStatus { PENDING, DONE, SKIPPED }

enum class ActionSource { MANUAL, AI, PHOTO_AI }

data class PlantationAction(
    val id: String = UUID.randomUUID().toString(),
    val plantationId: String,
    val plantTypeId: String? = null,
    val actionType: ActionType,
    val title: String,
    val notes: String = "",
    val scheduledAt: Instant,
    val status: ActionStatus = ActionStatus.PENDING,
    val calendarEventId: Long? = null,
    val calendarAccountEmail: String? = null,
    val source: ActionSource = ActionSource.MANUAL,
    val createdAt: Instant = Instant.now()
)
