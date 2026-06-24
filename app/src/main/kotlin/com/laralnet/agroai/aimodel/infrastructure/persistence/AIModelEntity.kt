package com.laralnet.agroai.aimodel.infrastructure.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.GemmaVersion
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.model.PromptWarningLevel

@Entity(tableName = "ai_models")
data class AIModelEntity(
    @PrimaryKey val id: String,
    val variant: ModelVariant,
    val version: GemmaVersion,
    val filePath: String?,
    val downloadState: DownloadState,
    val downloadProgressPercent: Int,
    val isActive: Boolean,
    val lastError: String? = null
)

@Entity(tableName = "prompt_templates")
data class PromptTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val content: String,
    val systemContext: String,
    val isEditable: Boolean,
    val isCustomized: Boolean,
    val warningLevel: PromptWarningLevel,
    val defaultContent: String
)
