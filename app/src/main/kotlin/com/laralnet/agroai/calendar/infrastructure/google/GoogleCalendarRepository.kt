package com.laralnet.agroai.calendar.infrastructure.google

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.laralnet.agroai.calendar.domain.model.CalendarEvent
import com.laralnet.agroai.calendar.domain.model.CreatedCalendarEvent
import com.laralnet.agroai.calendar.domain.model.GoogleCalendar
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject

class GoogleCalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarRepository {

    private val resolver: ContentResolver get() = context.contentResolver

    override suspend fun getCalendars(accountEmail: String): List<GoogleCalendar> {
        val calendars = mutableListOf<GoogleCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(accountEmail)

        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1) ?: continue
                val account = cursor.getString(2) ?: continue
                val isPrimary = cursor.getInt(3) == 1
                calendars.add(GoogleCalendar(id, account, name, isPrimary))
            }
        }
        return calendars
    }

    override suspend fun createEvent(event: CalendarEvent): Result<CreatedCalendarEvent> =
        runCatching {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.startAt.toEpochMilli())
                put(CalendarContract.Events.DTEND, event.endAt.toEpochMilli())
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }

            val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: error("Failed to insert calendar event")
            val eventId = ContentUris.parseId(uri)

            event.remindersMinutes.forEach { minutes ->
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    put(CalendarContract.Reminders.MINUTES, minutes)
                }
                resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            CreatedCalendarEvent(
                eventId = eventId,
                calendarId = event.calendarId,
                accountEmail = event.accountEmail
            )
        }

    override suspend fun deleteEvent(calendarId: Long, eventId: Long): Result<Unit> = runCatching {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        resolver.delete(uri, null, null)
        Unit
    }

    override suspend fun updateEvent(eventId: Long, event: CalendarEvent): Result<Unit> =
        runCatching {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.startAt.toEpochMilli())
                put(CalendarContract.Events.DTEND, event.endAt.toEpochMilli())
            }
            resolver.update(uri, values, null, null)
            Unit
        }
}
