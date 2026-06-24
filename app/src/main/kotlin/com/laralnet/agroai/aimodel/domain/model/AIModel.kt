package com.laralnet.agroai.aimodel.domain.model

import java.util.UUID

enum class GemmaVersion { GEMMA_3, GEMMA_4 }

enum class ModelVariant(
    val displayName: String,
    val approximateSizeGb: Double,
    val requiredRamGb: Double,
    val downloadUrl: String,
    val gemmaVersion: GemmaVersion
) {
    GEMMA3_1B("Gemma 3 1B", 2.0, 3.0, "https://huggingface.co/google/gemma-3-1b-it-litert-preview", GemmaVersion.GEMMA_3),
    GEMMA3_4B("Gemma 3 4B", 5.0, 6.0, "https://huggingface.co/google/gemma-3-4b-it-litert-preview", GemmaVersion.GEMMA_3),
    GEMMA3_12B("Gemma 3 12B", 14.0, 16.0, "https://huggingface.co/google/gemma-3-12b-it-litert-preview", GemmaVersion.GEMMA_3),
    GEMMA4_2B("Gemma 4 2B", 3.0, 4.0, "", GemmaVersion.GEMMA_4)
}

enum class DownloadState { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, FAILED }

data class AIModel(
    val id: String = UUID.randomUUID().toString(),
    val variant: ModelVariant,
    val version: GemmaVersion,
    val filePath: String? = null,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val downloadProgressPercent: Int = 0,
    val isActive: Boolean = false
)
