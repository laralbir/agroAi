package com.laralnet.agroai.aimodel.domain.repository

import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import kotlinx.coroutines.flow.Flow

interface AIModelRepository {
    fun observeAll(): Flow<List<AIModel>>
    suspend fun findById(modelId: String): AIModel?
    suspend fun findByVariant(variant: ModelVariant): AIModel?
    suspend fun findActive(): AIModel?
    suspend fun save(model: AIModel)
    suspend fun setActive(modelId: String)
    suspend fun delete(modelId: String)
    suspend fun findPromptTemplate(name: String): PromptTemplate?
    suspend fun findPromptTemplateById(id: String): PromptTemplate?
    suspend fun savePromptTemplate(template: PromptTemplate)
    fun observePromptTemplates(): Flow<List<PromptTemplate>>
}
