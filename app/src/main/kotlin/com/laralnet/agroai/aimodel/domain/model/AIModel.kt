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
    GEMMA3_1B(
        displayName = "Gemma 3 1B",
        approximateSizeGb = 0.6,
        requiredRamGb = 2.0,
        downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
        gemmaVersion = GemmaVersion.GEMMA_3,
        infoUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
        localFileName = "gemma3_1b.task",
        descriptionEs = "Modelo de texto compacto, ideal para dispositivos de gama media. Respuestas rápidas con menor consumo de RAM. Sin análisis de imagen.",
        descriptionEn = "Compact text model, ideal for mid-range devices. Fast responses with lower RAM usage. No image analysis.",
        isRecommended = false,
        supportsVision = false
    ),

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
    ),

    GEMMA4_E2B(
        displayName = "Gemma 4 E2B",
        approximateSizeGb = 2.5,
        requiredRamGb = 4.0,
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        gemmaVersion = GemmaVersion.GEMMA_4,
        infoUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
        localFileName = "gemma4_e2b.litertlm",
        descriptionEs = "Última generación de Gemma. Mayor precisión en diagnósticos agrícolas y mejor comprensión del contexto. Sin análisis de imagen.",
        descriptionEn = "Latest Gemma generation. Higher accuracy for agricultural diagnostics and better context understanding. No image analysis.",
        isRecommended = false,
        supportsVision = false
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
