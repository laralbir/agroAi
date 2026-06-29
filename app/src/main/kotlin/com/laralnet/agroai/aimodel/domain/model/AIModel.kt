package com.laralnet.agroai.aimodel.domain.model

import java.util.UUID

enum class GemmaVersion { GEMMA_3, GEMMA_3N, GEMMA_4 }

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
    val isRecommended: Boolean = false,
    /** True for models that include a vision encoder and can process real images. */
    val supportsVision: Boolean = false
) {
    GEMMA3N_E2B(
        displayName = "Gemma 3n E2B",
        approximateSizeGb = 2.0,
        requiredRamGb = 4.0,
        downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
        gemmaVersion = GemmaVersion.GEMMA_3N,
        infoUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview",
        localFileName = "gemma3n_e2b.task",
        descriptionEs = "Gemma 3n multimodal: analiza texto e imágenes reales. Incluye encoder visual SigLIP. Recomendado para análisis de fotos de cultivos.",
        descriptionEn = "Gemma 3n multimodal: processes both text and real images. Includes SigLIP visual encoder. Recommended for crop photo analysis.",
        isRecommended = true,
        supportsVision = true
    ),

    GEMMA3N_E4B(
        displayName = "Gemma 3n E4B",
        approximateSizeGb = 4.0,
        requiredRamGb = 6.0,
        downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task",
        gemmaVersion = GemmaVersion.GEMMA_3N,
        infoUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview",
        localFileName = "gemma3n_e4b.task",
        descriptionEs = "Gemma 3n multimodal de alta precisión. Análisis de imágenes y texto con mayor profundidad de razonamiento. Requiere dispositivo de gama alta.",
        descriptionEn = "High-accuracy multimodal Gemma 3n. Image and text analysis with deeper reasoning. Requires a high-end device.",
        isRecommended = false,
        supportsVision = true
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
