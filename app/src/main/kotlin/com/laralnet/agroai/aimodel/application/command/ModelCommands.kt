package com.laralnet.agroai.aimodel.application.command

import com.laralnet.agroai.aimodel.domain.model.ModelVariant

data class DownloadModelCommand(val variant: ModelVariant)
data class SetActiveModelCommand(val modelId: String)
data class DeleteModelCommand(val modelId: String)

data class AnalyzePhotoCommand(
    val imageUri: String,
    val plantationId: String?,
    val customPrompt: String? = null
)

data class SavePromptTemplateCommand(
    val templateId: String,
    val newContent: String
)
