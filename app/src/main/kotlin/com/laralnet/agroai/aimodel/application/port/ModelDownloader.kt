package com.laralnet.agroai.aimodel.application.port

import com.laralnet.agroai.aimodel.domain.model.ModelVariant

interface ModelDownloader {
    suspend fun enqueue(modelId: String, variant: ModelVariant)
    fun cancel(modelId: String)
}
