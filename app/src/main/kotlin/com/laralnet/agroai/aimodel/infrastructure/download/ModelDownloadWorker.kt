package com.laralnet.agroai.aimodel.infrastructure.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
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
        const val KEY_STATUS = "status"
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

        val modelsDir = applicationContext.getExternalFilesDir("models")
            ?: applicationContext.filesDir.resolve("models").also { it.mkdirs() }
        val destFile = File(modelsDir, "${variant.name.lowercase()}.task")

        runCatching {
            emit(0, "Connecting to HuggingFace…")

            val hfToken = inputData.getString(KEY_HF_TOKEN).orEmpty()
            val request = Request.Builder()
                .url(variant.downloadUrl)
                .apply { if (hfToken.isNotBlank()) header("Authorization", "Bearer $hfToken") }
                .build()

            emit(0, "Sending request to ${variant.downloadUrl.substringAfterLast("/")}")

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val reason = when (response.code) {
                        401 -> "Token inválido o caducado (HTTP 401)"
                        403 -> "Acceso denegado (HTTP 403) — necesitas aceptar las condiciones de Gemma en HuggingFace antes de poder descargar"
                        404 -> "Archivo no encontrado (HTTP 404) — la URL puede haber cambiado"
                        429 -> "Demasiadas solicitudes (HTTP 429) — espera un momento y vuelve a intentarlo"
                        else -> "Error HTTP ${response.code}"
                    }
                    error(reason)
                }

                val body = response.body ?: error("Respuesta vacía del servidor")
                val contentLength = body.contentLength()
                val totalMb = if (contentLength > 0) " (${contentLength / 1024 / 1024} MB)" else ""
                emit(0, "Descargando$totalMb…")

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
                                if (progress > lastReportedProgress) {
                                    lastReportedProgress = progress
                                    val downloadedMb = bytesRead / 1024 / 1024
                                    val totalMbVal = contentLength / 1024 / 1024
                                    emit(progress, "$downloadedMb MB / $totalMbVal MB")
                                    updateProgress(modelId, progress)
                                }
                            } else {
                                // Chunked: no content-length, show MB downloaded
                                val downloadedMb = bytesRead / 1024 / 1024
                                val prevMb = (bytesRead - read) / 1024 / 1024
                                if (downloadedMb != prevMb) {
                                    emit(0, "$downloadedMb MB descargados…")
                                }
                            }
                        }
                    }
                }
            }

            val existing = repository.findById(modelId)
            repository.save(
                (existing ?: AIModel(id = modelId, variant = variant, version = variant.gemmaVersion)).copy(
                    filePath = destFile.absolutePath,
                    downloadState = DownloadState.DOWNLOADED,
                    downloadProgressPercent = 100,
                    lastError = null
                )
            )

            Result.success(workDataOf(KEY_FILE_PATH to destFile.absolutePath))
        }.getOrElse { e ->
            destFile.delete()
            val errorMsg = e.message ?: "Error desconocido"
            val existing = repository.findById(modelId)
            repository.save(
                (existing ?: AIModel(id = modelId, variant = variant, version = variant.gemmaVersion)).copy(
                    filePath = null,
                    downloadState = DownloadState.FAILED,
                    downloadProgressPercent = 0,
                    lastError = errorMsg
                )
            )
            Result.failure(workDataOf(KEY_ERROR to errorMsg))
        }
    }

    private suspend fun emit(progress: Int, status: String) {
        setProgress(workDataOf(KEY_PROGRESS to progress, KEY_STATUS to status))
    }

    private suspend fun updateProgress(modelId: String, progress: Int) {
        val existing = repository.findById(modelId) ?: return
        repository.save(existing.copy(downloadProgressPercent = progress))
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(NOTIFICATION_ID, ModelDownloadNotification.create(applicationContext))
}
