package com.laralnet.agroai.photoanalysis.application.query

import com.laralnet.agroai.photoanalysis.domain.model.AnalysisRecord
import com.laralnet.agroai.photoanalysis.domain.repository.AnalysisRepository
import javax.inject.Inject

class GetAnalysisQuery @Inject constructor(
    private val repository: AnalysisRepository
) {
    suspend operator fun invoke(id: String): AnalysisRecord? = repository.findById(id)
}
