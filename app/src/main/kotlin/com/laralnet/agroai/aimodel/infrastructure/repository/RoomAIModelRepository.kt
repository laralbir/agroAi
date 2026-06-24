package com.laralnet.agroai.aimodel.infrastructure.repository

import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.model.PromptWarningLevel
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.persistence.AIModelEntity
import com.laralnet.agroai.aimodel.infrastructure.persistence.PromptTemplateEntity
import com.laralnet.agroai.aimodel.infrastructure.persistence.dao.AIModelDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomAIModelRepository @Inject constructor(
    private val dao: AIModelDao
) : AIModelRepository {

    override fun observeAll(): Flow<List<AIModel>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun findById(modelId: String): AIModel? = dao.findById(modelId)?.toDomain()

    override suspend fun findByVariant(variant: ModelVariant): AIModel? =
        dao.findByVariant(variant)?.toDomain()

    override suspend fun findActive(): AIModel? = dao.findActive()?.toDomain()

    override suspend fun save(model: AIModel) = dao.insert(model.toEntity())

    override suspend fun setActive(modelId: String) {
        dao.clearActive()
        dao.setActive(modelId)
    }

    override suspend fun delete(modelId: String) = dao.delete(modelId)

    override suspend fun findPromptTemplate(name: String): PromptTemplate? =
        dao.findPromptTemplate(name)?.toDomain()

    override suspend fun findPromptTemplateById(id: String): PromptTemplate? =
        dao.findPromptTemplateById(id)?.toDomain()

    override suspend fun savePromptTemplate(template: PromptTemplate) =
        dao.insertPromptTemplate(template.toEntity())

    override fun observePromptTemplates(): Flow<List<PromptTemplate>> =
        dao.observePromptTemplates().map { list -> list.map { it.toDomain() } }

    private fun AIModelEntity.toDomain() = AIModel(
        id = id, variant = variant, version = version,
        filePath = filePath, downloadState = downloadState,
        downloadProgressPercent = downloadProgressPercent, isActive = isActive,
        lastError = lastError
    )

    private fun AIModel.toEntity() = AIModelEntity(
        id = id, variant = variant, version = version,
        filePath = filePath, downloadState = downloadState,
        downloadProgressPercent = downloadProgressPercent, isActive = isActive,
        lastError = lastError
    )

    private fun PromptTemplateEntity.toDomain() = PromptTemplate(
        id = id, name = name, content = content,
        systemContext = systemContext, isEditable = isEditable,
        isCustomized = isCustomized, warningLevel = warningLevel,
        defaultContent = defaultContent
    )

    private fun PromptTemplate.toEntity() = PromptTemplateEntity(
        id = id, name = name, content = content,
        systemContext = systemContext, isEditable = isEditable,
        isCustomized = isCustomized, warningLevel = warningLevel,
        defaultContent = defaultContent
    )
}
