package com.laralnet.agroai.action.application.query

import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveActionsByPlantationQuery @Inject constructor(
    private val repository: PlantationActionRepository
) {
    operator fun invoke(plantationId: String): Flow<List<PlantationAction>> =
        repository.observeByPlantation(plantationId)
}

class ObserveUpcomingActionsQuery @Inject constructor(
    private val repository: PlantationActionRepository
) {
    operator fun invoke(): Flow<List<PlantationAction>> =
        repository.observeUpcoming()
}
