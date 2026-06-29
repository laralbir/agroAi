package com.laralnet.agroai.action.application.handler

import com.laralnet.agroai.action.application.command.CompleteActionCommand
import com.laralnet.agroai.action.application.command.DeleteActionCommand
import com.laralnet.agroai.action.application.command.ScheduleActionCommand
import com.laralnet.agroai.action.application.command.UpdateActionCommand
import com.laralnet.agroai.action.domain.event.PlantationActionCompleted
import com.laralnet.agroai.action.domain.event.PlantationActionDeleted
import com.laralnet.agroai.action.domain.event.PlantationActionScheduled
import com.laralnet.agroai.action.domain.model.ActionStatus
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.calendar.application.command.CreateCalendarEventCommand
import com.laralnet.agroai.calendar.application.command.DeleteCalendarEventCommand
import com.laralnet.agroai.calendar.application.handler.CreateCalendarEventHandler
import com.laralnet.agroai.calendar.application.handler.DeleteCalendarEventHandler
import com.laralnet.agroai.core.infrastructure.event.EventBus
import javax.inject.Inject

class ScheduleActionHandler @Inject constructor(
    private val repository: PlantationActionRepository,
    private val createCalendarEventHandler: CreateCalendarEventHandler,
    private val eventBus: EventBus
) {
    suspend fun handle(command: ScheduleActionCommand): Result<PlantationAction> = runCatching {
        var calendarEventId: Long? = null
        var savedAccountEmail: String? = null

        if (command.calendarAccountEmail != null) {
            createCalendarEventHandler.handle(
                CreateCalendarEventCommand(
                    accountEmail = command.calendarAccountEmail,
                    title = command.title,
                    description = command.notes,
                    startAt = command.scheduledAt,
                    endAt = command.scheduledAt.plusSeconds(3600)
                )
            ).onSuccess { created ->
                calendarEventId = created.eventId
                savedAccountEmail = created.accountEmail
            }
        }

        val action = PlantationAction(
            plantationId = command.plantationId,
            plantTypeId = command.plantTypeId,
            actionType = command.actionType,
            title = command.title,
            notes = command.notes,
            scheduledAt = command.scheduledAt,
            source = command.source,
            calendarEventId = calendarEventId,
            calendarAccountEmail = savedAccountEmail ?: command.calendarAccountEmail
        )
        repository.save(action)
        eventBus.publish(PlantationActionScheduled(actionId = action.id, plantationId = action.plantationId))
        action
    }
}

class CompleteActionHandler @Inject constructor(
    private val repository: PlantationActionRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: CompleteActionCommand): Result<Unit> = runCatching {
        val action = repository.findById(command.id)
            ?: error("PlantationAction ${command.id} not found")
        repository.save(action.copy(status = ActionStatus.DONE))
        eventBus.publish(PlantationActionCompleted(actionId = action.id, plantationId = action.plantationId))
    }
}

class DeleteActionHandler @Inject constructor(
    private val repository: PlantationActionRepository,
    private val deleteCalendarEventHandler: DeleteCalendarEventHandler,
    private val eventBus: EventBus
) {
    suspend fun handle(command: DeleteActionCommand): Result<Unit> = runCatching {
        val action = repository.findById(command.id)
            ?: error("PlantationAction ${command.id} not found")
        val calEventId = action.calendarEventId
        if (calEventId != null) {
            deleteCalendarEventHandler.handle(DeleteCalendarEventCommand(eventId = calEventId))
        }
        repository.delete(action.id)
        eventBus.publish(PlantationActionDeleted(actionId = action.id, plantationId = action.plantationId))
    }
}

class UpdateActionHandler @Inject constructor(
    private val repository: PlantationActionRepository
) {
    suspend fun handle(command: UpdateActionCommand): Result<Unit> = runCatching {
        val action = repository.findById(command.id)
            ?: error("PlantationAction ${command.id} not found")
        repository.save(
            action.copy(
                actionType = command.actionType,
                title = command.title,
                notes = command.notes,
                scheduledAt = command.scheduledAt
            )
        )
    }
}
