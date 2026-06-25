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
    val localFileName: String = "",
    val descriptionEs: String = "",
    val descriptionEn: String = "",
    val isRecommended: Boolean = false
) {
    GEMMA3_1B(
        displayName = "Gemma 3 1B",
        approximateSizeGb = 0.6,
        requiredRamGb = 2.0,
        downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        gemmaVersion = GemmaVersion.GEMMA_3,
        infoUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
        localFileName = "gemma3_1b.task",
        descriptionEs = "Modelo compacto, ideal para dispositivos de gama media. Respuestas rápidas con menor consumo de RAM.",
        descriptionEn = "Compact model, ideal for mid-range devices. Fast responses with lower RAM usage.",
        isRecommended = false
    ),

    GEMMA4_E2B(
        displayName = "Gemma 4 E2B",
        approximateSizeGb = 2.5,
        requiredRamGb = 4.0,
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        gemmaVersion = GemmaVersion.GEMMA_4,
        infoUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
        localFileName = "gemma4_e2b.litertlm",
        descriptionEs = "Última generación de Gemma. Mayor precisión en diagnósticos agrícolas y mejor comprensión del contexto.",
        descriptionEn = "Latest Gemma generation. Higher accuracy for agricultural diagnostics and better context understanding.",
        isRecommended = true
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
