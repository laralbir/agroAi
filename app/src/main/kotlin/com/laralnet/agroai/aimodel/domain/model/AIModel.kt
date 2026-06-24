package com.laralnet.agroai.aimodel.domain.model

import java.util.UUID

enum class GemmaVersion { GEMMA_3, GEMMA_4 }

enum class ModelVariant(
    val displayName: String,
    val approximateSizeGb: Double,
    val requiredRamGb: Double,
    val downloadUrl: String,
    val gemmaVersion: GemmaVersion,
    // Page the user visits to accept Gemma terms and get a token
    val infoUrl: String = ""
) {
    // litert-community/Gemma3-1B-IT — gemma3-1b-it-int4.task (555 MB, Android-compatible)
    GEMMA3_1B(
        "Gemma 3 1B", 0.6, 2.0,
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        GemmaVersion.GEMMA_3,
        "https://huggingface.co/litert-community/Gemma3-1B-IT"
    )
    // Gemma 4 variants: litert-community only publishes .web.task (WebAssembly) files,
    // which are not compatible with the Android MediaPipe Tasks GenAI SDK.
    // Re-add entries here once Android-compatible .task files are available.
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
