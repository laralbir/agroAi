package com.laralnet.agroai.treatment.application.handler

import com.laralnet.agroai.calendar.domain.model.CalendarEvent
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class MigrateCalendarAccountHandler @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val calendarRepository: CalendarRepository
) {
    data class Result(val migrated: Int, val failed: Int)

    /**
     * Migrates calendar events for all upcoming treatments from [oldEmail] to [newEmail].
     * For each treatment with a calendar event in the old account:
     *   1. Creates a new event in the primary calendar of the new account.
     *   2. Deletes the old event (best effort).
     *   3. Updates the treatment record with the new event ID and account email.
     */
    suspend fun handle(oldEmail: String, newEmail: String): Result {
        val newCalendars = runCatching { calendarRepository.getCalendars(newEmail) }
            .getOrElse { return Result(0, 0) }
        val primaryCalendar = newCalendars.firstOrNull { it.isPrimary } ?: newCalendars.firstOrNull()
            ?: return Result(0, 0)

        val allUpcoming = treatmentRepository.observeUpcoming().first()
        val toMigrate = allUpcoming.filter {
            it.calendarAccountEmail == oldEmail && it.calendarEventId != null
        }

        var migrated = 0
        var failed = 0

        for (treatment in toMigrate) {
            val event = CalendarEvent(
                calendarId = primaryCalendar.id,
                accountEmail = newEmail,
                title = treatment.title,
                description = treatment.description,
                startAt = treatment.scheduledAt,
                endAt = treatment.scheduledAt.plusSeconds(3600)
            )
            calendarRepository.createEvent(event)
                .onSuccess { created ->
                    // Delete from old account (best effort — we don't have the old calendarId stored)
                    val oldCalendars = runCatching { calendarRepository.getCalendars(oldEmail) }.getOrElse { emptyList() }
                    for (oldCal in oldCalendars) {
                        runCatching { calendarRepository.deleteEvent(oldCal.id, treatment.calendarEventId!!) }
                    }
                    // Update treatment record
                    treatmentRepository.save(
                        treatment.copy(
                            calendarAccountEmail = newEmail,
                            calendarEventId = created.eventId
                        )
                    )
                    migrated++
                }
                .onFailure { failed++ }
        }

        return Result(migrated, failed)
    }
}
