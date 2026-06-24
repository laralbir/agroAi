package com.laralnet.agroai.aimodel.infrastructure.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.GemmaVersion
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AIModelRepository,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MODEL_VARIANT = "model_variant"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR = "error"
        const val KEY_HF_TOKEN = "hf_token"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val variantName = inputData.getString(KEY_MODEL_VARIANT) ?: return@withContext Result.failure()
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return@withContext Result.failure()
        val variant = runCatching { ModelVariant.valueOf(variantName) }.getOrNull()
            ?: return@withContext Result.failure()

        val destFile = File(
            applicationContext.getExternalFilesDir("models"),
            "${variant.name.lowercase()}.task"
        )

        runCatching {
            setProgress(workDataOf(KEY_PROGRESS to 0))

            val hfToken = inputData.getString(KEY_HF_TOKEN).orEmpty()
            val request = Request.Builder()
                .url(variant.downloadUrl)
                .apply { if (hfToken.isNotBlank()) header("Authorization", "Bearer $hfToken") }
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code} — check your HuggingFace token in Settings")

                val body = response.body ?: error("Empty response body")
                val contentLength = body.contentLength()
                var bytesRead = 0L
                var lastReportedProgress = -1

                destFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (contentLength > 0) {
                                val progress = (bytesRead * 100 / contentLength).toInt()
                                // Only report when progress changes by at least 1% to avoid flooding Room
                                if (progress > lastReportedProgress) {
                                    lastReportedProgress = progress
                                    setProgress(workDataOf(KEY_PROGRESS to progress))
                                    updateProgress(modelId, variant, progress)
                                }
                            }
                        }
                    }
                }
            }

            // Read existing record to preserve isActive and other fields
            val existing = repository.findById(modelId)
            repository.save(
                (existing ?: AIModel(id = modelId, variant = variant, version = variant.gemmaVersion)).copy(
                    filePath = destFile.absolutePath,
                    downloadState = DownloadState.DOWNLOADED,
                    downloadProgressPercent = 100
                )
            )

            Result.success(workDataOf(KEY_FILE_PATH to destFile.absolutePath))
        }.getOrElse { e ->
            destFile.delete()
            val existing = repository.findById(modelId)
            repository.save(
                (existing ?: AIModel(id = modelId, variant = variant, version = variant.gemmaVersion)).copy(
                    filePath = null,
                    downloadState = DownloadState.FAILED,
                    downloadProgressPercent = 0
                )
            )
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    private suspend fun updateProgress(modelId: String, variant: ModelVariant, progress: Int) {
        val existing = repository.findById(modelId) ?: return
        repository.save(existing.copy(downloadProgressPercent = progress))
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            ModelDownloadNotification.create(applicationContext)
        )
    }

}
