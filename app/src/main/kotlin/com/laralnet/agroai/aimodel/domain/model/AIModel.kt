package com.laralnet.agroai.aimodel.domain.model

import java.util.UUID

enum class GemmaVersion { GEMMA_3, GEMMA_4 }

enum class ModelVariant(
    val displayName: String,
    val approximateSizeGb: Double,
    val requiredRamGb: Double,
    val downloadUrl: String,
    val gemmaVersion: GemmaVersion,
    val infoUrl: String = "",
    // Local filename to use on disk. Needed because .litertlm and .task have different extensions.
    val localFileName: String = ""
) {
    // litert-community/Gemma3-1B-IT — gemma3-1b-it-int4.task (555 MB, Android-compatible)
    GEMMA3_1B(
        displayName = "Gemma 3 1B",
        approximateSizeGb = 0.6,
        requiredRamGb = 2.0,
        downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        gemmaVersion = GemmaVersion.GEMMA_3,
        infoUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
        localFileName = "gemma3_1b.task"
    ),

    // litert-community/gemma-4-E2B-it-litert-lm — gemma-4-E2B-it.litertlm (~2.5 GB, LiteRT LM format)
    // Requires Backend.CPU or Backend.GPU (new LiteRT path); incompatible with Backend.DEFAULT.
    GEMMA4_E2B(
        displayName = "Gemma 4 E2B",
        approximateSizeGb = 2.5,
        requiredRamGb = 4.0,
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        gemmaVersion = GemmaVersion.GEMMA_4,
        infoUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
        localFileName = "gemma4_e2b.litertlm"
    )
}

enum class DownloadState { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, FAILED }

data class AIModel(
    val id: String = UUID.randomUUID().toString(),
    val variant: ModelVariant,
    val version: GemmaVersion,
    val filePath: String? = null,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val downloadProgressPercent: Int = 0,
    val isActive: Boolean = false,
    val lastError: String? = null
)
