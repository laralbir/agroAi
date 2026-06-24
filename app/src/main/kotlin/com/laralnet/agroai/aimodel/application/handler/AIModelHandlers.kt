package com.laralnet.agroai.aimodel.application.handler

import com.laralnet.agroai.aimodel.application.command.DeleteModelCommand
import com.laralnet.agroai.aimodel.application.command.DownloadModelCommand
import com.laralnet.agroai.aimodel.application.command.SavePromptTemplateCommand
import com.laralnet.agroai.aimodel.application.command.SetActiveModelCommand
import com.laralnet.agroai.aimodel.application.port.ModelDownloader
import com.laralnet.agroai.aimodel.domain.event.ModelActivated
import com.laralnet.agroai.aimodel.domain.event.ModelDeleted
import com.laralnet.agroai.aimodel.domain.event.ModelDownloadStarted
import com.laralnet.agroai.aimodel.domain.event.PromptTemplateUpdated
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.core.infrastructure.event.EventBus
import java.io.File
import java.util.UUID
import javax.inject.Inject

class DownloadModelHandler @Inject constructor(
    private val repository: AIModelRepository,
    private val downloader: ModelDownloader,
    private val eventBus: EventBus
) {
    suspend fun handle(command: DownloadModelCommand): Result<String> = runCatching {
        val existing = repository.findByVariant(command.variant)

        // Idempotent: return existing id if already downloaded
        if (existing?.downloadState == DownloadState.DOWNLOADED) return@runCatching existing.id

        val modelId = existing?.id ?: UUID.randomUUID().toString()
        repository.save(
            AIModel(
                id = modelId,
                variant = command.variant,
                version = command.variant.gemmaVersion,
                downloadState = DownloadState.DOWNLOADING
            )
        )
        downloader.enqueue(modelId, command.variant)
        eventBus.publish(ModelDownloadStarted(modelId = modelId, variant = command.variant))
        modelId
    }
}

class SetActiveModelHandler @Inject constructor(
    private val repository: AIModelRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: SetActiveModelCommand): Result<Unit> = runCatching {
        val model = repository.findById(command.modelId)
            ?: error("Model ${command.modelId} not found")
        check(model.downloadState == DownloadState.DOWNLOADED) {
            "Cannot activate model ${command.modelId}: not downloaded (state=${model.downloadState})"
        }
        repository.setActive(command.modelId)
        eventBus.publish(ModelActivated(modelId = command.modelId))
    }
}

class DeleteModelHandler @Inject constructor(
    private val repository: AIModelRepository,
    private val downloader: ModelDownloader,
    private val eventBus: EventBus
) {
    suspend fun handle(command: DeleteModelCommand): Result<Unit> = runCatching {
        val model = repository.findById(command.modelId)
            ?: error("Model ${command.modelId} not found")

        if (model.downloadState == DownloadState.DOWNLOADING) {
            downloader.cancel(model.id)
        }

        model.filePath?.let { path -> File(path).apply { if (exists()) delete() } }

        repository.delete(command.modelId)
        eventBus.publish(ModelDeleted(modelId = command.modelId))
    }
}

class SavePromptTemplateHandler @Inject constructor(
    private val repository: AIModelRepository,
    private val eventBus: EventBus
) {
    suspend fun handle(command: SavePromptTemplateCommand): Result<Unit> = runCatching {
        val existing = repository.findPromptTemplateById(command.templateId)
            ?: error("PromptTemplate ${command.templateId} not found")
        check(existing.isEditable) { "PromptTemplate ${command.templateId} is not editable" }

        repository.savePromptTemplate(existing.copy(content = command.newContent, isCustomized = true))
        eventBus.publish(PromptTemplateUpdated(templateId = existing.id, templateName = existing.name))
    }
}
