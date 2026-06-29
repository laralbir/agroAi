package com.laralnet.agroai.action.application.command

import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionType
import java.time.Instant

data class ScheduleActionCommand(
    val plantationId: String,
    val plantTypeId: String? = null,
    val actionType: ActionType,
    val title: String,
    val notes: String = "",
    val scheduledAt: Instant,
    val source: ActionSource = ActionSource.MANUAL,
    val calendarAccountEmail: String? = null
)

data class CompleteActionCommand(
    val id: String,
    val notes: String = ""
)

data class DeleteActionCommand(val id: String)

data class UpdateActionCommand(
    val id: String,
    val actionType: ActionType,
    val title: String,
    val notes: String,
    val scheduledAt: Instant
)
