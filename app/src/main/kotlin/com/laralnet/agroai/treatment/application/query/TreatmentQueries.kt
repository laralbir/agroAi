package com.laralnet.agroai.treatment.application.query

import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

class ObserveTreatmentsByPlantationQuery @Inject constructor(
    private val repository: TreatmentRepository
) {
    operator fun invoke(plantationId: String): Flow<List<Treatment>> =
        repository.observeByPlantation(plantationId)
}

class ObserveUpcomingTreatmentsQuery @Inject constructor(
    private val repository: TreatmentRepository
) {
    operator fun invoke(): Flow<List<Treatment>> = repository.observeUpcoming()
}

class GetTreatmentQuery @Inject constructor(
    private val repository: TreatmentRepository
) {
    suspend operator fun invoke(id: String): Treatment? = repository.findById(id)
}

class ObserveTreatmentsByMonthQuery @Inject constructor(
    private val repository: TreatmentRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<Treatment>> {
        val zone = ZoneId.systemDefault()
        val start: Instant = yearMonth.atDay(1).atStartOfDay(zone).toInstant()
        val end: Instant = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        return repository.observeByDateRange(start, end)
    }
}
