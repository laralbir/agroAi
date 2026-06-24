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
    ),
    // litert-community/Gemma3-4B-IT — only -web.task variants exist (not compatible with
    // Android tasks-genai SDK). URL empty until Android .task becomes available.
    GEMMA3_4B(
        "Gemma 3 4B", 2.6, 4.0,
        "",
        GemmaVersion.GEMMA_3,
        "https://huggingface.co/litert-community/Gemma3-4B-IT"
    ),
    // litert-community/Gemma3-12B-IT — same situation as 4B (web-only .task files).
    GEMMA3_12B(
        "Gemma 3 12B", 7.6, 12.0,
        "",
        GemmaVersion.GEMMA_3,
        "https://huggingface.co/litert-community/Gemma3-12B-IT"
    ),
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
    val isActive: Boolean = false,
    val lastError: String? = null
)
