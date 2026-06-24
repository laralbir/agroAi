package com.laralnet.agroai.aimodel.infrastructure.download

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.laralnet.agroai.aimodel.application.port.ModelDownloader
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WorkManagerModelDownloader @Inject constructor(
    private val workManager: WorkManager,
    private val dataStore: DataStore<Preferences>
) : ModelDownloader {

    override suspend fun enqueue(modelId: String, variant: ModelVariant) {
        val token = dataStore.data.first()[KEY_HF_TOKEN] ?: ""
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(
                workDataOf(
                    ModelDownloadWorker.KEY_MODEL_ID to modelId,
                    ModelDownloadWorker.KEY_MODEL_VARIANT to variant.name,
                    ModelDownloadWorker.KEY_HF_TOKEN to token
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

    companion object {
        val KEY_HF_TOKEN = stringPreferencesKey("hf_token")
    }
}
