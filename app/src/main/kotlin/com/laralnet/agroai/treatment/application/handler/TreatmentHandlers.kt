package com.laralnet.agroai.treatment.application.handler

import com.laralnet.agroai.calendar.application.command.CreateCalendarEventCommand
import com.laralnet.agroai.calendar.application.command.DeleteCalendarEventCommand
import com.laralnet.agroai.calendar.application.handler.CreateCalendarEventHandler
import com.laralnet.agroai.calendar.application.handler.DeleteCalendarEventHandler
import com.laralnet.agroai.core.infrastructure.event.EventBus
import com.laralnet.agroai.treatment.application.command.CompleteTreatmentCommand
import com.laralnet.agroai.treatment.application.command.DeleteTreatmentCommand
import com.laralnet.agroai.treatment.application.command.ScheduleTreatmentCommand
import com.laralnet.agroai.treatment.domain.event.TreatmentCompleted
import com.laralnet.agroai.treatment.domain.event.TreatmentDeleted
import com.laralnet.agroai.treatment.domain.event.TreatmentScheduled
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.model.TreatmentRecord
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import java.time.Instant
import javax.inject.Inject

class ScheduleTreatmentHandler @Inject constructor(
    private val repository: TreatmentRepository,
    private val createCalendarEventHandler: CreateCalendarEventHandler,
    private val eventBus: EventBus
) {
    suspend fun handle(command: ScheduleTreatmentCommand): Result<Treatment> = runCatching {
        var calendarEventId: Long? = null
        var savedAccountEmail: String? = null

        if (command.addToCalendar && command.calendarAccountEmail != null) {
            createCalendarEventHandler.handle(
                CreateCalendarEventCommand(
                    accountEmail = command.calendarAccountEmail,
                    title = command.title,
                    description = command.description,
                    startAt = command.scheduledAt,
                    endAt = command.scheduledAt.plusSeconds(3600)
                )
            ).onSuccess { created ->
                calendarEventId = created.eventId
                savedAccountEmail = created.accountEmail
            }
        }

        val treatment = Treatment(
            plantationId = command.plantationId,
            type = command.type,
            title = command.title,
            description = command.description,
            scheduledAt = command.scheduledAt,
            status = TreatmentStatus.PENDING,
            calendarEventId = calendarEventId,
            calendarAccountEmail = savedAccountEmail ?: command.calendarAccountEmail
        )
        repository.save(treatment)
        eventBus.publish(TreatmentScheduled(treatmentId = treatment.id, plantationId = command.plantationId))
        treatment
    }
}

class CompleteTreatmentHandler @Inject constructor(
    private val repository: TreatmentRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: CompleteTreatmentCommand): Result<Unit> = runCatching {
        val treatment = repository.findById(command.treatmentId)
            ?: error("Treatment ${command.treatmentId} not found")
        repository.save(treatment.copy(status = TreatmentStatus.DONE))
        repository.saveRecord(
            TreatmentRecord(
                treatmentId = command.treatmentId,
                plantationId = treatment.plantationId,
                executedAt = Instant.now(),
                notes = command.notes,
                photoUri = command.photoUri,
                aiAnalysisResult = command.aiAnalysisResult
            )
        )
        eventBus.publish(TreatmentCompleted(treatmentId = command.treatmentId, plantationId = treatment.plantationId))
    }
}

class DeleteTreatmentHandler @Inject constructor(
    private val repository: TreatmentRepository,
    private val deleteCalendarEventHandler: DeleteCalendarEventHandler,
    private val eventBus: EventBus
) {
    suspend fun handle(command: DeleteTreatmentCommand): Result<Unit> = runCatching {
        val treatment = repository.findById(command.treatmentId)
            ?: error("Treatment ${command.treatmentId} not found")
        val calEventId = treatment.calendarEventId
        if (calEventId != null) {
            deleteCalendarEventHandler.handle(DeleteCalendarEventCommand(eventId = calEventId))
        }
        repository.delete(command.treatmentId)
        eventBus.publish(TreatmentDeleted(treatmentId = command.treatmentId, plantationId = treatment.plantationId))
    }
}
