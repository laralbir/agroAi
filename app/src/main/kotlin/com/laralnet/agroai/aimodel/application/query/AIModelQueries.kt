package com.laralnet.agroai.aimodel.application.query

import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveModelsQuery @Inject constructor(
    private val repository: AIModelRepository
) {
    operator fun invoke(): Flow<List<AIModel>> = repository.observeAll()
}

class GetActiveModelQuery @Inject constructor(
    private val repository: AIModelRepository
) {
    suspend operator fun invoke(): AIModel? = repository.findActive()
}

class ObservePromptTemplatesQuery @Inject constructor(
    private val repository: AIModelRepository
) {
    operator fun invoke(): Flow<List<PromptTemplate>> = repository.observePromptTemplates()
}

// Returns the template from DB, or null if it doesn't exist yet.
// Callers are responsible for seeding defaults (e.g. PromptTemplate.photoAnalysisDefault()).
class GetPromptTemplateQuery @Inject constructor(
    private val repository: AIModelRepository
) {
    suspend operator fun invoke(name: String): PromptTemplate? = repository.findPromptTemplate(name)
}
