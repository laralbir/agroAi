package com.laralnet.agroai.calendar.domain.event

import com.laralnet.agroai.core.domain.event.DomainEvent

class CalendarEventCreated(val calendarEventId: Long, val treatmentId: String?) : DomainEvent()
class CalendarEventUpdated(val calendarEventId: Long) : DomainEvent()
class CalendarEventDeleted(val calendarEventId: Long) : DomainEvent()
