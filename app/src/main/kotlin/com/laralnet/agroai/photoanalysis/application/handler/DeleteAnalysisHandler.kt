package com.laralnet.agroai.photoanalysis.application.handler

import com.laralnet.agroai.photoanalysis.domain.repository.AnalysisRepository
import javax.inject.Inject

class DeleteAnalysisHandler @Inject constructor(
    private val repository: AnalysisRepository
) {
    suspend fun handle(id: String) = repository.delete(id)
}
