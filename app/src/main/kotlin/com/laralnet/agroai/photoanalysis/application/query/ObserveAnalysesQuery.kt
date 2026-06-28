package com.laralnet.agroai.photoanalysis.application.query

import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import com.laralnet.agroai.photoanalysis.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAnalysesQuery @Inject constructor(
    private val repository: AnalysisRepository
) {
    operator fun invoke(plantationId: String?): Flow<List<AnalysisRecord>> =
        if (plantationId != null)
            repository.observeByPlantation(plantationId)
        else
            repository.observeAll()
}
