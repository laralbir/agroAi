package com.laralnet.agroai.aimodel.infrastructure.download

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.laralnet.agroai.aimodel.application.port.ModelDownloader
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import javax.inject.Inject

class WorkManagerModelDownloader @Inject constructor(
    private val workManager: WorkManager
) : ModelDownloader {

    override fun enqueue(modelId: String, variant: ModelVariant) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(
                workDataOf(
                    ModelDownloadWorker.KEY_MODEL_ID to modelId,
                    ModelDownloadWorker.KEY_MODEL_VARIANT to variant.name
                )
            )
            .build()
        workManager.enqueueUniqueWork(
            workName(modelId),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun cancel(modelId: String) {
        workManager.cancelUniqueWork(workName(modelId))
    }

    private fun workName(modelId: String) = "model_download_$modelId"
}
